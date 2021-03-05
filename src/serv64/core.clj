(ns serv64.core
  (:use serv64.menu serv64.utils serv64.protocols serv64.xmodem)
  (:import
    [io.netty.buffer ByteBuf Unpooled]
    [io.netty.channel ChannelInboundHandlerAdapter ChannelInitializer]
    [io.netty.bootstrap ServerBootstrap]
    [io.netty.channel.nio NioEventLoopGroup]
    [io.netty.channel.socket.nio NioServerSocketChannel]
    [java.io File FileFilter FileInputStream]))

; mostly messy I/O operations and action interpreter

(defn get-files []
  "returns an array of java.io.File objects"
  (vec (.listFiles (File. "files") (proxy [FileFilter] [] (accept [dir] (.isFile dir))))))

(defn get-file-names []
  (sort (map #(.getName %) (get-files))))

(defn get-file-listing []
  (let [file-names (get-file-names)
        lines (map-indexed (fn [idx file-name] (format "%4d. %s\n" (+ 1 idx) file-name)) file-names)]
    (clojure.string/join lines)))

(defn read-file [^File file]
  "File -> byte[]"
  (let [bytes (byte-array (.length file))
        file-input-stream (FileInputStream. file)]
    (.read file-input-stream bytes)
    (.close file-input-stream)
    bytes))

(defn get-file-bytes [index]
  (let [files (get-files)
        zero-idx (- index 1)]
    (if (<= index (count files))
      (read-file (get files zero-idx))
      nil)))

(defn write-output [ctx output]
  (.writeAndFlush ctx (Unpooled/wrappedBuffer (to-byte-array output))))

(declare do-activate)

(def server-changer (set [:push :pop :replace :xmodem]))

(defn process-actions [server-stack actions ctx]
  (doseq [[action value] actions]
    (case action
      :send (write-output ctx value)
      :push (swap! server-stack #(conj % value))
      :pop (swap! server-stack pop)
      :replace (swap! server-stack (fn [stack] (conj (pop stack) value)))
      :list-files (write-output ctx (get-file-listing))
      :disconnect (.close ctx)
      :xmodem (let [file-bytes (get-file-bytes value)]
                (if (not (nil? file-bytes))
                  (swap! server-stack #(conj % (make-xmodem file-bytes)))
                  (write-output ctx "File error.\n"))))
    (if (contains? server-changer action)
      (do-activate server-stack ctx))))

(defn do-activate [server-stack ctx]
    (let [actions (activate (last @server-stack) (System/nanoTime))]
      (process-actions server-stack actions ctx)))

(defn do-updates [server-stack ^ByteBuf msg ctx]
  "call the recv function on the current server and interpret the actions"
  (let [msgBytes (byte-array (.readableBytes msg))]
    (.readBytes msg msgBytes)
    (let [actions (recv (last @server-stack) msgBytes (System/nanoTime))]
      (process-actions server-stack actions ctx))))

(defn make-handler
  "Create a ChannelInboundHandlerAdaptor instance to handle inbound tcp connections"
  []
  (let [server-stack (atom [main-menu])]
    (proxy [ChannelInboundHandlerAdapter] []
      (channelActive [ctx]
        (println "channelActive")
        (write-output ctx main-menu-text))
      (channelRead [ctx ^ByteBuf msg]
        (println "channelRead")
        (do-updates server-stack msg ctx))
      (channelInactive [ctx]
        (println "channelInactive"))
      (exceptionCaught [ctx cause]
        ; Close the connection when an exception is raised
        (.printStackTrace cause)
        (.close ctx)))))

(defn run-server
  "Create and run TCP listening server"
  [port]
  (let [boss-group (NioEventLoopGroup.)
        worker-group (NioEventLoopGroup.)]
    ((try
       (let [b (-> (ServerBootstrap.)
                   (.group boss-group worker-group)
                   (.channel NioServerSocketChannel)
                   (.childHandler (proxy [ChannelInitializer] []
                                    (initChannel [ch]
                                      (-> ch
                                          (.pipeline)
                                          (.addLast (make-handler)))))))
             f (-> b
                   (.bind port)
                   (.sync))]
         (-> f
             (.channel)
             (.closeFuture)
             (.sync)))
       (finally
         (.shutdownGracefully worker-group)
         (.shutdownGracefully boss-group))))))

(defn -main []
  (println "Starting server.")
  (run-server 8080))
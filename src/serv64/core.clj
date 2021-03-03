(ns serv64.core
  (:use serv64.menu serv64.utils serv64.protocols)
  (:import
    [io.netty.buffer ByteBuf Unpooled]
    [io.netty.channel ChannelInboundHandlerAdapter ChannelInitializer ChannelOption ChannelFutureListener]
    [io.netty.bootstrap ServerBootstrap]
    [io.netty.channel.nio NioEventLoopGroup]
    [io.netty.channel.socket.nio NioServerSocketChannel]
    [java.io File FileFilter]))

; mostly messy I/O operations and action interpreter

(defn get-files []
  "returns an array of java.io.File objects"
  (vec (.listFiles (File. "files") (proxy [FileFilter] [] (accept [dir] (.isFile dir))))))

(defn get-file-listing []
  (let [file-names (map #(.getName %) (get-files))]
    (str (clojure.string/join "\n" file-names) "\n")))

(defn write-output [ctx output]
  (.writeAndFlush ctx (Unpooled/wrappedBuffer (to-byte-array output))))

(defn do-updates [server-stack ^ByteBuf msg ctx]
  "call the recv function on the current server and interpret the actions"
  (let [msgBytes (byte-array (.readableBytes msg))]
    (.readBytes msg msgBytes)
    (let [actions (recv (last @server-stack) msgBytes (System/nanoTime))]
      (doseq [[action value] actions]
        (case action
          :send (write-output ctx value)
          :push (swap! server-stack #(conj % value))
          :pop (swap! server-stack pop)
          :list-files (write-output ctx (get-file-listing))
          :disconnect (.close ctx))))))

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
(ns serv64.core
  (:use serv64.actions serv64.utils)
  (:import
    [io.netty.buffer ByteBuf Unpooled]
    [io.netty.channel ChannelInboundHandlerAdapter ChannelInitializer ChannelOption ChannelFutureListener]
    [io.netty.bootstrap ServerBootstrap]
    [io.netty.channel.nio NioEventLoopGroup]
    [io.netty.channel.socket.nio NioServerSocketChannel]
    [serv64.actions Menu]))

; mostly messy I/O operations and top-level interpretation of Actions goes here

(defn write-output [ctx output]
  (.writeAndFlush ctx (Unpooled/wrappedBuffer (to-byte-array output))))

(defn write-and-close [ctx output]
  (.addListener (write-output ctx output)
                (proxy [ChannelFutureListener] []
                  (operationComplete [f] (.close ctx)))))

(defn do-update [state-agent in-byte ctx]
  (swap! state-agent
         (fn [state]
           (let [action (recv state in-byte (System/nanoTime))]
             (case (:action action)
               :update (do (write-output ctx (:output action)) (:state action))
               :disconnect (do (write-and-close ctx (:output action)) (:state action))
               :ignore state)))))

(defn do-updates [state-agent ^ByteBuf msg ctx]
  (while (.isReadable msg)
    (do-update state-agent (.readByte msg) ctx)))

(defn make-handler
  "Create a ChannelInboundHandlerAdaptor instance to handle inbound tcp connections"
  []
  (let [menu-state (atom nil)]
    (proxy [ChannelInboundHandlerAdapter] []
      (channelActive [ctx]
        (println "channelActive")
        (swap! menu-state (fn [ignored]
                            (write-output ctx main-menu-text)
                            (Menu. :main))))
      (channelRead [ctx ^ByteBuf msg]
        (println "channelRead")
        (do-updates menu-state msg ctx))
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
         (.shutdownGracefully boss-group))))
    ))

(defn -main []
  (println "Starting server.")
  (run-server 8080))
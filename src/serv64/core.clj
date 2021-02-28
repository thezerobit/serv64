(ns serv64.core
  (:import
    [io.netty.buffer ByteBuf]
    [io.netty.channel ChannelHandlerContext ChannelInboundHandlerAdapter ChannelFuture ChannelInitializer
                      ChannelOption EventLoopGroup]
    [io.netty.bootstrap ServerBootstrap]
    [io.netty.channel.nio NioEventLoopGroup]
    [io.netty.channel.socket SocketChannel]
    [io.netty.channel.socket.nio NioServerSocketChannel]))

(defn make-handler
  "Create a ChannelInboundHandlerAdaptor instance to handle inbound tcp connections"
  []
  (proxy [ChannelInboundHandlerAdapter] []
    (channelRead [ctx msg]
      ; Discard the received data silently
      (.release msg))
    (exceptionCaught [ctx cause]
      ; Close the connection when an exception is raised
      (.printStackTrace cause)
      (.close ctx))))

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
                                          (.addLast (make-handler))))))
                   (.childOption ChannelOption/SO_BACKLOG true))
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

(run-server 8080)
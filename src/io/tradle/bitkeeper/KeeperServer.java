package io.tradle.bitkeeper;

import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import net.tomp2p.connection.*;
import net.tomp2p.connection.Bindings.*;
import net.tomp2p.futures.*;
import net.tomp2p.p2p.*;
import net.tomp2p.peers.*;

import org.jboss.netty.bootstrap.*;
import org.jboss.netty.channel.socket.nio.*;

/**
 * An HTTP server that serves key value pares to and from DHT.
 * 
 */
public class KeeperServer {
  private static final Random RND = new Random(System.currentTimeMillis());
  
  public static void main(String[] args) throws Exception {
    String myIpAddress = args[0];
    int httpPort = Integer.parseInt(args[1]);
    int myDhtPort = Integer.parseInt(args[2]);
    int masterDhtPort = Integer.parseInt(args[3]);
    String masterDhtIpAddress = args[4];
    
    Peer me = createOwnDhtPeer(myDhtPort, myIpAddress, masterDhtPort, masterDhtIpAddress);
    
    ExecutorService threadPool = Executors.newCachedThreadPool();
    
    // bootstrap to DHT network
    threadPool.execute(new DhtBoostrapper(me, masterDhtPort, masterDhtIpAddress));
    
    // Configure the server.
    ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));

    // Set up the event pipeline factory.
    bootstrap.setPipelineFactory(new ServerPipelineFactory(me, threadPool));

    // Bind and start to accept incoming connections.
    bootstrap.bind(new InetSocketAddress(httpPort));

  }

  public static Peer createOwnDhtPeer(int myPort, String myIp, int masterPort, String masterIp) throws Exception {
    InetAddress myIpAddrress = Inet4Address.getByName(myIp);
    Bindings b = new Bindings(Protocol.IPv4, myIpAddrress, myPort, myPort);
    // b.addInterface("eth0");
    Peer client = new PeerMaker(new Number160(RND))
                  .setPorts(myPort)
                  .setEnableIndirectReplication(true)
                  .setBindings(b)
                  .makeAndListen();
    System.out.println("DHT client started and Listening to: " + DiscoverNetworks.discoverInterfaces(b));
    System.out.println("DHT client address visible to outside is " + client.getPeerAddress());
    return client;
  }
  
  static class DhtBoostrapper implements Runnable {
    int masterPort;
    InetAddress masterAddress;
    PeerAddress pa;
    Peer myself;
    boolean bootstrapped;
    
    DhtBoostrapper(Peer myself, int masterPort, String masterIp) throws Exception {
      masterAddress = Inet4Address.getByName(masterIp);
      this.masterPort = masterPort;
      pa = new PeerAddress(Number160.ZERO, masterAddress, masterPort, masterPort);
      this.myself = myself;
    }

    @Override
    public void run() {
      while (true) {
        if (!bootstrapped) {
          // Future Bootstrap - slave
          FutureBootstrap futureBootstrap = myself.bootstrap().setInetAddress(masterAddress).setPorts(masterPort).start();
          futureBootstrap.awaitUninterruptibly();
          if (futureBootstrap.isSuccess()) {
            System.out.println("succeded to bootstrap to DHC");
            bootstrapped = true;
          } else {
            System.out.println("failed " + futureBootstrap.getFailedReason());
            bootstrapped = false;
          }
        }
        else {
          // ping
          FutureChannelCreator fcc = myself.getConnectionBean().getConnectionReservation().reserve( 1 );
          fcc.awaitUninterruptibly();

          ChannelCreator cc = fcc.getChannelCreator();

          FutureResponse fr1 = myself.getHandshakeRPC().pingTCP(pa, cc);
          fr1.awaitUninterruptibly();

          if (fr1.isSuccess()) {
            //System.out.println("peer online T:" + pa);
          }  
          else {
            System.out.println("peer offline " + pa);
            bootstrapped = false;
          } 
          // FutureResponse fr2 = master.getHandshakeRPC().pingUDP(pa, cc);
          // fr2.awaitUninterruptibly();

          myself.getConnectionBean().getConnectionReservation().release(cc);

          // if (fr2.isSuccess())
          // System.out.println("peer online U:" + pa);
          // else
          // System.out.println("offline " + pa);

        }
        try {
          Thread.sleep(3000);
        } catch (InterruptedException e) {
          break;
        }
      }  
    }
  }
}

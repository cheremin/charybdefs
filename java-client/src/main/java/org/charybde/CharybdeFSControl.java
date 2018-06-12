package org.charybde;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.charybde.CharybdeFSControl.Method.*;

/**
 * @author ruslan
 *         created 28/04/2018 at 15:22
 */
public class CharybdeFSControl implements AutoCloseable {
	private static final int DEFAULT_PORT = 9090;
	private static final String DEFAULT_HOST = "localhost";

	private final String host;
	private final int port;

	private final transient TTransport transport;
	private final server.Iface client;

	public CharybdeFSControl() throws TException {
		this( DEFAULT_HOST, DEFAULT_PORT );
	}

	public CharybdeFSControl( final String host, final int port ) throws TException {
		checkArgument( port > 0, "port[" + port + "] must be >0" );
		this.host = requireNonNull( host, "host" );
		this.port = port;

		transport = new TSocket( this.host, this.port );
		final TTransport transport = new TSocket( host, port );
		transport.open();
		final TProtocol protocol = new TBinaryProtocol( transport );

		client = new server.Client( protocol );
		client.clear_all_faults();
	}

	public List<String> methodsAvailable() throws TException {
		return client.get_methods();
	}

	public void clearAllFaults() throws TException {
		client.clear_all_faults();
	}

	public void clearFault( final Method method ) throws TException {
		client.clear_fault( method.methodName() );
	}

	/**
	 * See server.cc::error_inject() source for describing how do exactly all those
	 * options are implemented and interfered.
	 *
	 * @param methods              the list of methods to operate on
	 * @param random               Must we return random errno
	 * @param probability          Fault probability over 100 000
	 * @param errorNo              specific errno to return
	 * @param victimFileNameRegExp A regexp matching a victim file
	 * @param killCaller           Kill -9 the caller process
	 * @param delayUs              Delay to inject in the fs calls
	 */
	public void setFault( final Set<Method> methods,
	                      final int errorNo,
	                      final boolean random,
	                      final int probability,
	                      final String victimFileNameRegExp,
	                      final boolean killCaller,
	                      final int delayUs ) throws TException {
		client.set_fault(
				methods.stream().map( Method::methodName ).collect( Collectors.toList() ),
				random,
				errorNo,
				probability,
				victimFileNameRegExp,
				killCaller,
				delayUs,
				/* autoDelay = */false //Not implemented yet: Will be used to simulate SSDs latencies
		);
	}

	/**
	 * @param random               Must we return random errno
	 * @param probability          Fault probability over 100 000
	 * @param errorNo              specific errno to return
	 * @param victimFileNameRegExp A regexp matching a victim file
	 * @param killCaller           Kill -9 the caller process
	 * @param delayUs              Delay to inject in the fs calls
	 */
	public void setAllFaults( final int errorNo,
	                          final boolean random,
	                          final int probability,
	                          final String victimFileNameRegExp,
	                          final boolean killCaller,
	                          final int delayUs ) throws TException {
		client.set_all_fault(
				random,
				errorNo,
				probability,
				victimFileNameRegExp,
				killCaller,
				delayUs,
				/* autoDelay = */false //Not implemented yet: Will be used to simulate SSDs latencies
		);
	}


	@Override
	public void close() throws Exception {
		if( transport.isOpen() ) {
			transport.close();
		}
	}

	//TODO RC: it is better to create enum for valid IO error codes:
	// it seems reasonable to take range from (EIO, EXFULL)
	//http://www-numi.fnal.gov/offline_software/srt_public_context/WebDocs/Errors/unix_system_errors.html

	/** Enum for FileSystem (FUSE) methods to which Charybde may apply its magic */
	public enum Method {
		getattr( "getattr" ),
		readlink( "readlink" ),
		mknod( "mknod" ),
		mkdir( "mkdir" ),
		unlink( "unlink" ),
		rmdir( "rmdir" ),
		symlink( "symlink" ),
		rename( "rename" ),
		link( "link" ),
		chmod( "chmod" ),
		chown( "chown" ),
		truncate( "truncate" ),
		open( "open" ),
		read( "read" ),
		write( "write" ),
		statfs( "statfs" ),
		flush( "flush" ),
		release( "release" ),
		fsync( "fsync" ),
		setxattr( "setxattr" ),
		getxattr( "getxattr" ),
		listxattr( "listxattr" ),
		removexattr( "removexattr" ),
		opendir( "opendir" ),
		readdir( "readdir" ),
		releasedir( "releasedir" ),
		fsyncdir( "fsyncdir" ),
		access( "access" ),
		create( "create" ),
		ftruncate( "ftruncate" ),
		fgetattr( "fgetattr" ),
		lock( "lock" ),
		bmap( "bmap" ),
		ioctl( "ioctl" ),
		poll( "poll" ),
		write_buf( "write_buf" ),
		read_buf( "read_buf" ),
		flock( "flock" ),
		fallocate( "fallocate" );

		private final String name;

		Method( final String name ) {
			this.name = name;
		}

		public String methodName() {
			return name;
		}
	}

	public static void main( String[] args ) throws Exception {
		if( args.length != 1 ) {
			System.out.println( "Please enter 'simple' or 'secure'" );
			System.exit( 0 );
		}

		try (final CharybdeFSControl charybde = new CharybdeFSControl()) {
			System.out.println( charybde.methodsAvailable() );

			charybde.setFault(
					EnumSet.of( flush, fsync, fsyncdir ),
					/*errorNo = */          0,
					/*random = */           false,
					/*probability/100_000*/ 100000,
					/*fileNameRegExp = */   "",
					/*killCaller = */       true,
					/*delayUs = */          500000
			);
			charybde.setFault(
					EnumSet.of( flush, fsync, fsyncdir ),
					/*errorNo = */          0,
					/* random = */          false,
					/*probability/100_000*/ 99000,
					/*fileNameRegExp = */   "",
					/*killCaller = */       true,
					/*delayUs = */          500000
			);
			charybde.clearAllFaults();
		}

//		try {
//			final TTransport transport = new TSocket( "localhost", 9090 );
//			transport.open();
//			try {
//				final TProtocol protocol = new TBinaryProtocol( transport );
//				final server.Client client = new server.Client( protocol );
//				System.out.println( client.get_methods() );
//
//				client.set_fault(
//						asList( "flush", "fsync", "fsyncdir" ),
//						false, 0, 100000, "", true, 500000, false
//				);
//				client.set_fault(
//						asList( "flush", "fsync", "fsyncdir" ),
//						false, 0, 99000, "", true, 500000, false
//				);
//				client.clear_all_faults();
//			} finally {
//				transport.close();
//			}
//		} catch( TException x ) {
//			x.printStackTrace();
//		}
	}
}

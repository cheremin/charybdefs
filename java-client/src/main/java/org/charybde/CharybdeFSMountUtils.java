package org.charybde;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author ruslan
 *         created 11/06/2018 at 17:54
 */
public abstract class CharybdeFSMountUtils {

	private static final File CHARYBDE_EXECUTABLE = new File(
			System.getProperty( "charybde.executable", "charybdefs" )
	);
	private static final boolean DEBUG_FUSE = Boolean.getBoolean( "charybde.debug-fuse" );

	private CharybdeFSMountUtils() {
		throw new AssertionError( "Not for instantiation" );
	}

	public static AutoCloseable mount( final File mountOverDirectory,
	                                   final File dataDirectory ) throws IOException, TimeoutException, InterruptedException {
		return mount( CHARYBDE_EXECUTABLE, mountOverDirectory, dataDirectory );
	}

	public static AutoCloseable mount( final File charybdeExecutable,
	                                   final File mountOverDirectory,
	                                   final File dataDirectory ) throws IOException, TimeoutException, InterruptedException {
		killPreviousInstancesIfExist( charybdeExecutable );

		if( !mountOverDirectory.exists() ) {
			if( !mountOverDirectory.mkdirs() ) {
				throw new IOException( "Can't create [" + mountOverDirectory.getAbsolutePath() + "]" );
			}
		}
		//mounted directory must be empty
		cleanDirectory( mountOverDirectory );

		final String[] commands;
		if( DEBUG_FUSE ) {
			//starting charybdefs with FUSE debug: -f -d
			commands = new String[] {
					charybdeExecutable.getAbsolutePath(),
					"-f", "-d",
					mountOverDirectory.getAbsolutePath(),
					"-omodules=subdir,subdir=" + dataDirectory.getAbsolutePath(),
					"> charybde.out", "2>&1", "&"
			};
		} else {
			commands = new String[] {
					charybdeExecutable.getAbsolutePath(),
					mountOverDirectory.getAbsolutePath(),
					"-omodules=subdir,subdir=" + dataDirectory.getAbsolutePath()
			};
		}

		final ProcessUtils.ProcessResult result = ProcessUtils.runOneShot(
				1, SECONDS,
				commands
		);
		if( result.exitCode == 0 ) {
			//mounted charybde
			return () -> unmount( mountOverDirectory );
		} else {
			throw new IOException( "CharybdeFS mount ["
					                       + mountOverDirectory.getAbsolutePath()
					                       + " -> "
					                       + dataDirectory.getAbsolutePath() + "] failed: "
					                       + result
			);
		}

	}

	public static void cleanDirectory( final File directory ) {
		if( !directory.isDirectory() ) {
			throw new IllegalArgumentException( "[" + directory + "] is not a directory" );
		}
		final File[] files = directory.listFiles();
		if( files != null ) {
			for( final File file : files ) {
				if( file.isFile() ) {
					file.delete();
				} else if( file.isDirectory() ) {
					cleanDirectory( directory );
					file.delete();
				}
			}
		}
	}

	private static void unmount( final File mountOverDirectory ) throws InterruptedException, TimeoutException, IOException {
		final ProcessUtils.ProcessResult result = ProcessUtils.runOneShot(
				1, SECONDS,
				"fusermount", "-u", mountOverDirectory.getAbsolutePath()
		);
		if( result.exitCode == 0 ) {
			//success
		} else {
			throw new IOException( "Failed to unmount [" + mountOverDirectory.getAbsolutePath() + "]: " + result );
		}
	}

	private static void killPreviousInstancesIfExist( final File charybdeExecutable ) throws InterruptedException, TimeoutException, IOException {
		//charybdefs itself doesn't have any protection against running it multiple
		// times -- but it doesn't work correctly this way. It may be misleading, since
		// charybdefs process is started, and seems like OK, mounted directory is indeed
		// mounted, and so on. But you have no control over this charybde instance,
		// since controlling is done via thrift connection on fixed network port (9090)
		// -- and only 1 charybde instance will be able to bind to it, while others
		// silently fail doing it.
		// Thus it is important to kill previous instances

		final String executableName = charybdeExecutable.getName();
		final ProcessUtils.ProcessResult result = ProcessUtils.runOneShot(
				1, SECONDS,
				"findmnt",
				executableName
		);
		if( result.exitCode == 0 ) {
			if( result.stdout.contains( executableName ) ) {
				//there are previous instances: killall them
				ProcessUtils.runOneShot(
						1, SECONDS,
						"killall", executableName
				);
			}
		}
	}
}

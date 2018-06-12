package org.charybde;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.Math.min;


/**
 * @author ruslan
 *         created 18/05/2018
 */
public class ProcessUtils {
	private static final long DEFAULT_WAIT_MS = 100;
	private static final long DEFAULT_WAIT_NS = TimeUnit.MILLISECONDS.toNanos( DEFAULT_WAIT_MS );

	public static ProcessResult runOneShot( final long timeout,
	                                        final TimeUnit unit,
	                                        final String... commands ) throws IOException, InterruptedException, TimeoutException {
		final Process process = new ProcessBuilder( commands )
				.redirectErrorStream( true )
				.start();

		final long timeoutNs = unit.toNanos( timeout );
		final byte[] buffer = new byte[1024];
		final long startedAtNs = System.nanoTime();
		try (final InputStream stdout = process.getInputStream()) {
			final ByteArrayOutputStream target = new ByteArrayOutputStream();
			while( true ) {
				final int maxBytesToPump = 10 << 10;//TODO limit max target capacity!
				//important to ask aliveness _before_ pumping
				final boolean processAlive = process.isAlive();
				final int bytesPumped = pumpNonBlocking( stdout, target, buffer, DEFAULT_WAIT_NS, maxBytesToPump );
				if( processAlive ) {
					final long elapsedNs = System.nanoTime() - startedAtNs;
					if( elapsedNs > timeoutNs ) {
						process.destroyForcibly();
						final String stdoutAsString = new String( target.toByteArray(), "ASCII" );
						throw new TimeoutException( Arrays.toString( commands ) + " is not terminated in " + timeout + " " + unit + ": stdout so far [" + stdoutAsString + "]" );
					} else if( bytesPumped == 0 ) {
						Thread.sleep( DEFAULT_WAIT_MS );
					}//do not sleep if stdout contains something to read
				} else {
					if( bytesPumped == 0 ) {
						final String stdoutAsString = new String( target.toByteArray(), "ASCII" );
						final int exitValue = process.exitValue();
						return new ProcessResult(
								exitValue,
								stdoutAsString
						);
					}//if process is dead, don't worry about timeout, try to exhaust stdout!
				}
			}
		}
	}

	private static int pumpNonBlocking( final InputStream source,
	                                    final OutputStream target,
	                                    final byte[] buffer,
	                                    final long timeoutNs,
	                                    final int maxBytesToPump ) throws IOException {
		int totalRead = 0;
		for( final long startedNs = System.nanoTime();
		     System.nanoTime() - startedNs < timeoutNs; ) {
			final int available = source.available();
			if( available <= 0 ) {
				break;
			}

			final int allowedToRead = maxBytesToPump - totalRead;
			if( allowedToRead > 0 ) {
				final int toRead = min( allowedToRead, min( available, buffer.length ) );
				final int actualRead = source.read( buffer, 0, toRead );
				target.write( buffer, 0, actualRead );
				totalRead += actualRead;
			}
		}
		return totalRead;
	}

	public static class ProcessResult {
		public final int exitCode;
		public final String stdout;

		private ProcessResult( final int exitCode,
		                       final String stdout ) {
			this.exitCode = exitCode;
			this.stdout = stdout;
		}
	}
}

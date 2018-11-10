import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author ruslan
 *         created 11/11/2018 at 00:33
 */
public class BadFileDescriptor {
	public static void main( String[] args ) throws IOException {
		final File charybdeDirectory = new File( args[0] );
		final File testFile = new File( charybdeDirectory, "test" );
		final long writtenValue = 0x1234_5678_9ABC_DEF1L;

		try (final RandomAccessFile raf = new RandomAccessFile( testFile, "rw" )) {
			raf.writeLong( writtenValue );
			raf.seek( 0 );
			final long readValue = raf.readLong();
			if( readValue != writtenValue ) {
				throw new AssertionError( "Value read (" + readValue + ") is not same as just written (" + writtenValue + ")" );
			}
		}
	}
}

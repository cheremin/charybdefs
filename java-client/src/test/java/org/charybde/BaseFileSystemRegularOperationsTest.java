package org.charybde;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.*;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author ruslan
 *         created 12/06/2018 at 23:08
 */
public abstract class BaseFileSystemRegularOperationsTest {
	@ClassRule
	public static final TemporaryFolder temporaryFolder = new TemporaryFolder();


	private static File target;

	@BeforeClass
	public static void setUpFolder() throws Exception {
		target = temporaryFolder.newFolder( "target" );
	}

	protected static File targetFolder() {
		return target;
	}

	@Test
	public void symlinkCouldBeCreatedAndIdentifiedAsSymlink() throws Exception {
		//Verifies [https://github.com/scylladb/charybdefs/issues/10]

		final File testFile = new File( targetFolder(), "test" );
		assertThat( testFile.createNewFile(),
		            is( true )
		);
		assertThat( testFile.isFile(),
		            is( true )
		);

		final File link = new File( targetFolder(), "test.link" );

		Files.createSymbolicLink( link.toPath(), testFile.toPath() );

		assertThat( link.exists(),
		            is( true )
		);
		assertThat( Files.isSymbolicLink( link.toPath() ),
		            is( true )
		);
	}

	@Test
	public void multipleSymlinksAreNotInterfere() throws Exception {
		//Verifies [https://github.com/scylladb/charybdefs/issues/11]
		final String[] fileNames = { "test-1", "test-2-1", "test-3-2-1" };
		final String[] linkNames = { "test-1.link", "test-2-1.link", "test-3-2-1.link" };


//		final List<File> files = new ArrayList<>();
		final List<File> symlinks = new ArrayList<>();
		for( int i = 0; i < fileNames.length; i++ ) {
			final String fileName = fileNames[i];
			final String linkName = linkNames[i];

			final File testFile = new File( targetFolder(), fileName );
			final File symlink = new File( targetFolder(), linkName );

			Files.createSymbolicLink( symlink.toPath(), testFile.toPath() );

//			files.add( testFile );
			symlinks.add( symlink );
		}

		for( final File symlink : symlinks ) {
			assertThat(
					"Symlink-ed file must exist",
					Files.exists( symlink.toPath() ),
					is( true )
			);
		}
	}

	@Test
	public void valueWrittenCouldBeReadBackUnchanged() throws Exception {
		final File testFile = new File( targetFolder(), "test" );
		final long writtenValue = 0x1234_5678_9ABC_DEF1L;
		try (final RandomAccessFile raf = new RandomAccessFile( testFile, "rw" )) {
			raf.writeLong( writtenValue );
			raf.seek( 0 );
			final long readValue = raf.readLong();
			assertThat( "Value read is same as just written",
			            readValue,
			            is( writtenValue ) );
		}
	}
}

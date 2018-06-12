package org.charybde;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.*;

import org.junit.*;
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


	private static File targetDirectory;

	@BeforeClass
	public static void createTargetFolder() throws Exception {
		targetDirectory = temporaryFolder.newFolder( "targetDirectory" );
	}

	@Before
	public void cleanup() throws Exception {
		CharybdeFSMountUtils.cleanDirectory( targetDirectory() );
	}

	protected static File targetDirectory() {
		return targetDirectory;
	}

	@Test
	public void symlinkCouldBeCreatedAndIdentifiedAsSymlink() throws Exception {
		//Verifies [https://github.com/scylladb/charybdefs/issues/10]

		final File testFile = new File( targetDirectory(), "test" );
		assertThat( testFile.createNewFile(),
		            is( true )
		);
		assertThat( testFile.isFile(),
		            is( true )
		);

		final File link = new File( targetDirectory(), "test.link" );

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

			final File testFile = new File( targetDirectory(), fileName );
			final File symlink = new File( targetDirectory(), linkName );
			testFile.createNewFile();

			Files.createSymbolicLink( symlink.toPath(), testFile.toPath() );

//			files.add( testFile );
			symlinks.add( symlink );
		}

		for( final File symlink : symlinks ) {
			assertThat(
					"Symlink-ed file[" + symlink.getAbsolutePath() + "] must exist",
					Files.exists( symlink.toPath() ),
					is( true )
			);
		}
	}

	@Test
	public void valueWrittenCouldBeReadBackUnchanged() throws Exception {
		final File testFile = new File( targetDirectory(), "test" );
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

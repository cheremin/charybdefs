package org.charybde;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author ruslan
 *         created 12/06/2018 at 18:34
 */
public class CharybdeFileSystemRegularOperationsTest {

	@ClassRule
	public static final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private AutoCloseable charybde = null;
	private File mountOverFolder;

	@Before
	public void setUp() throws Exception {
		mountOverFolder = temporaryFolder.newFolder( "mounted" );
		final File dataFolder = temporaryFolder.newFolder( "data" );
		charybde = CharybdeFSMountUtils.mount( mountOverFolder, dataFolder );
	}

	@After
	public void tearDown() throws Exception {
		if( charybde != null ) {
			charybde.close();
		}
	}

	@Test
	public void link() throws Exception {
		final File testFile = new File( mountOverFolder, "test" );
		assertThat( testFile.createNewFile(),
		            is( true )
		);
		assertThat( testFile.isFile(),
		            is( true )
		);

		final File link = new File( mountOverFolder, "test.link" );

		Files.createSymbolicLink( link.toPath(), testFile.toPath() );

		assertThat( link.exists(),
		            is( true )
		);
		assertThat( Files.isSymbolicLink( link.toPath() ),
		            is( true )
		);
	}

	@Test
	public void valueWrittenCouldBeReadBackUnchanged() throws Exception {
		final File testFile = new File( mountOverFolder, "test" );
		final long writtenValue = 0x1234_5678_9ABC_DEF1L;
		try (final RandomAccessFile raf = new RandomAccessFile( testFile, "rw" )) {
			raf.writeLong( writtenValue );
			raf.seek( 0 );
			final long readValue = raf.readLong();
			assertThat( readValue,
			            is( writtenValue ) );
		}
	}
}

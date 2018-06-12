package org.charybde;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author ruslan
 *         created 12/06/2018 at 18:34
 */
public class CharybdeFileSystemRegularOperationsTest extends BaseFileSystemRegularOperationsTest {

	private static File dataFolder;

	@BeforeClass
	public void setupFolders() throws IOException {
		dataFolder = temporaryFolder.newFolder( "data" );
	}

	private AutoCloseable charybde = null;

	@Before
	public void setUp() throws Exception {
		CharybdeFSMountUtils.cleanDirectory( dataFolder );
		CharybdeFSMountUtils.cleanDirectory( targetDirectory() );
		charybde = CharybdeFSMountUtils.mount( targetDirectory(), dataFolder );
	}

	@After
	public void tearDown() throws Exception {
		if( charybde != null ) {
			charybde.close();
		}
	}

}

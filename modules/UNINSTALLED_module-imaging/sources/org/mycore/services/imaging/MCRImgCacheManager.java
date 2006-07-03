// ============================================== 
//  												
// Module-Imaging 1.0, 05-2006  		
// +++++++++++++++++++++++++++++++++++++			
//  												
// Andreas Trappe 	- idea, concept
// Chi Vu Huu		- concept, development
//
// $Revision$ $Date$ 
// ============================================== 

package org.mycore.services.imaging;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.mycore.common.MCRException;
import org.mycore.datamodel.ifs.MCRDirectory;
import org.mycore.datamodel.ifs.MCRFile;
import org.mycore.datamodel.ifs.MCRFileNodeServlet;
import org.mycore.datamodel.ifs.MCRFilesystemNode;

/*******************************************************************************
 * The MCRImgCacheManager deliver the method for getting data into and out of
 * the cache structure which lies in the IFS.<br>
 * The cache structure is just an one level flat tree:<br>
 * |--> [imgCache] --> [cached Image File] --> [versions of image]<br>
 * <br>
 * 
 * Cached image file is a subfolder of the root node imgCache in IFS. The name
 * is composed the following:<br>
 * ownerID + absolute path + filename<br>
 * in absolute path the "/" is replaced by "%20"
 * 
 * 
 * @version 0.01pre 03/01/2006
 * @author Vu Huu Chi
 * 
 */

public class MCRImgCacheManager implements CacheManager {
	public static final String THUMB = "Thumb";

	public static final String CACHE = "Cache";

	public static final String ORIG = "Orig";

	private Logger LOGGER = Logger.getLogger(MCRFileNodeServlet.class.getName());

	protected MCRDirectory cacheInIFS = null;

	public MCRImgCacheManager() {
		if ((cacheInIFS = MCRDirectory.getRootDirectory("imgCache")) == null)
			try {
				cacheInIFS = new MCRDirectory("imgCache", "imgCache");
			} catch (Exception e) {
				throw new MCRException(e.getMessage());
			}
	}

	public void getImage(MCRFile image, Dimension size, OutputStream imageData) {
		getImage(image, dimToString(size), imageData);
	}

	public void getImage(MCRFile image, String filename, OutputStream imageData) {
		MCRFilesystemNode cachedImg = cacheInIFS.getChildByPath(buildPath(image) + "/" + filename);

		if (cachedImg != null && cachedImg instanceof MCRFile) {
			Stopwatch timer = new Stopwatch();

			timer.start();
			((MCRFile) cachedImg).getContentTo(imageData);
			timer.stop();
			LOGGER.info("getContentTo: " + timer.getElapsedTime());
		} else
			throw new MCRException("Could not load " + image.getName() + "from cache!");
	}

	public InputStream getImageAsInputStream(MCRFile image, String filename) throws IOException {
		MCRFilesystemNode cachedImg = cacheInIFS.getChildByPath(buildPath(image) + "/" + filename);

		if (cachedImg != null && cachedImg instanceof MCRFile) {
			return ((MCRFile) cachedImg).getContentAsInputStream();
		} 
		else {
			return null;
		}
	}

	public void saveImage(MCRFile image, Dimension size, InputStream imageData) {
		saveImage(image, dimToString(size), imageData);
	}

	public void saveImage(MCRFile image, String filename, InputStream imageData) {
		String path = buildPath(image);
		MCRFilesystemNode node = cacheInIFS.getChildByPath(path);
		MCRDirectory cachedImg = null;

		LOGGER.info("****************************************");
		LOGGER.info("* PATH for img in Cache: " + path);
		LOGGER.info("****************************************");
		
		if (node != null && node instanceof MCRDirectory)
			cachedImg = (MCRDirectory) node;
		else
			cachedImg = new MCRDirectory(path, cacheInIFS);

		try {
			MCRFile cachedImgIFS = new MCRFile(filename + "lock", cachedImg);

			cachedImgIFS.setContentFrom(imageData);
			cachedImgIFS.setName(filename);
		} catch (Exception e) {
			throw new MCRException("Could not save Image " + image.getName() + " as " + filename + " in cache!");
		}
	}

	public void deleteImage(MCRFile image, Dimension size) {
		deleteImage(image, dimToString(size));
	}

	public void deleteImage(MCRFile image, String filename) {
		MCRFilesystemNode cachedImg = cacheInIFS.getChildByPath(buildPath(image) + "/" + filename);

		if (cachedImg != null && cachedImg instanceof MCRFile)
			((MCRFile) cachedImg).delete();
		else
			throw new MCRException("Could not delete " + image.getName() + "from cache!");

	}

	public void deleteImage(MCRFile image) {
		MCRFilesystemNode cachedImg = cacheInIFS.getChildByPath(buildPath(image));

		if (cachedImg != null && cachedImg instanceof MCRDirectory)
			((MCRDirectory) cachedImg).delete();
		else
			throw new MCRException("Could not delete " + image.getName() + "from cache!");

	}

	private String buildPath(MCRFile image) {
		String ownerID = image.getOwnerID();
		String absPath = image.getAbsolutePath().replaceAll("/", "%20");
		
		return ownerID + absPath;
	}

	// return 'width'x'height' as String
	private String dimToString(Dimension size) {
		return size.width + "x" + size.height;
	}

	public boolean existInCache(MCRFile image, Dimension size) {
		return existInCache(image, dimToString(size));
	}

	public boolean existInCache(MCRFile image, String filename) {
		MCRFilesystemNode cachedImg = cacheInIFS.getChildByPath(buildPath(image) + "/" + filename);

		if (cachedImg != null && cachedImg instanceof MCRFile)
			return true;
		else
			return false;
	}

	public boolean existInCache(MCRFile image) {
		return existInCache(image, THUMB) && existInCache(image, CACHE) && existInCache(image, ORIG);
	}

	public int getImgWidth(MCRFile image) {
		int width = 0;

		try {
			Document addData = image.getAdditionalData();

			if (addData != null) {
				width = (new Integer(addData.getRootElement().getChild("imageSize").getChild("width").getText())).intValue();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return width;
	}

	public int getImgHeight(MCRFile image) {
		int height = 0;

		try {
			Document addData = image.getAdditionalData();

			if (addData != null) {
				height = (new Integer(addData.getRootElement().getChild("imageSize").getChild("height").getText())).intValue();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return height;
	}

	public void setImgSize(MCRFile image, int width, int height) {
		try {
			Document addData = image.getAdditionalData();
			if (addData == null) {
				addData = new Document((new Element("ImageMetaData")).addContent((new Element("imageSize"))));
				Element elem = addData.getRootElement().getChild("imageSize");
				elem.addContent((new Element("width")).setText(String.valueOf(width)));
				elem.addContent((new Element("height")).setText(String.valueOf(height)));
				image.setAdditionalData(addData);

				LOGGER.info("Writing additional data for " + image.getName() + " complete!");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

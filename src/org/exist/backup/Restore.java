package org.exist.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Observable;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.exist.security.SecurityManager;
import org.exist.security.User;
import org.exist.xmldb.CollectionImpl;
import org.exist.xmldb.CollectionManagementServiceImpl;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.UserManagementService;
import org.exist.xquery.XPathException;
import org.exist.xquery.value.DateTimeValue;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.XMLDBException;

/**
 * Restore.java
 * 
 * @author Wolfgang Meier
 */
public class Restore extends DefaultHandler {

	private File contents;
	private String uri;
	private String username;
	private String pass;
	private String adminPass = null;
	private XMLReader reader;
	private CollectionImpl current;
	private Stack stack = new Stack();
	private RestoreDialog dialog = null;

	public final static String NS = "http://exist.sourceforge.net/NS/exist";

	/**
	 * Constructor for Restore.
	 */
	public Restore(String user, String pass, File contents, String uri)
		throws ParserConfigurationException, SAXException {
		this.username = user;
		this.pass = pass;
		this.uri = uri;
		SAXParserFactory saxFactory = SAXParserFactory.newInstance();
		saxFactory.setNamespaceAware(true);
		saxFactory.setValidating(false);
		SAXParser sax = saxFactory.newSAXParser();
		reader = sax.getXMLReader();
		reader.setContentHandler(this);

		stack.push(contents);

		// check if /db/system is in the backup. We have to process
		// this first to create users.
		File dir = contents.getParentFile();
		if (dir.isDirectory() && dir.getName().equals("db")) {
			File sys =
				new File(
					dir.getAbsolutePath()
						+ File.separatorChar
						+ "system"
						+ File.separatorChar
						+ "__contents__.xml");
			// put /db/system on top of the stack
			if (sys.canRead()) {
				System.out.println("found /db/system. It will be processed first.");
				stack.push(sys);
			}
		}
	}

	public Restore(String user, String pass, File contents)
		throws ParserConfigurationException, SAXException {
		this(user, pass, contents, "xmldb:exist://");
	}

	public void restore(boolean showGUI, JFrame parent)
		throws XMLDBException, FileNotFoundException, IOException, SAXException {
		if (showGUI) {
			dialog = new RestoreDialog(parent, "Restoring data ...", false);
			dialog.setVisible(true);
			Thread restoreThread = new Thread() {
				public void run() {
					while (!stack.isEmpty()) {
						try {
							contents = (File) stack.pop();
							reader.parse(new InputSource(new FileInputStream(contents)));
						} catch (FileNotFoundException e) {
							dialog.displayMessage(e.getMessage());
						} catch (IOException e) {
							dialog.displayMessage(e.getMessage());
						} catch (SAXException e) {
							dialog.displayMessage(e.getMessage());
						}
					}
					dialog.setVisible(false);
				}
			};
			restoreThread.start();
			if(parent == null) {
				while (restoreThread.isAlive()) {
					synchronized (this) {
						try {
							wait(20);
						} catch (InterruptedException e) {
						}
					}
				}
			}
		} else {
			while(!stack.isEmpty()) {
				contents = (File) stack.pop();
				String sysId = contents.toURI().toASCIIString();
				InputSource is = new InputSource(sysId);
				is.setEncoding("UTF-8");
				System.out.println("restoring " + sysId);
				reader.parse(is);
			}
		}
		
		// at the end of the restore process, set the admin password to the
		// password restored from the backup. Up to here, we still used the old password.
		Collection root = DatabaseManager.getCollection(uri + "/db", username, pass);
		UserManagementService service =
			(UserManagementService) root.getService("UserManagementService", "1.0");
		User admin = service.getUser(SecurityManager.DBA_USER);
		admin.setPasswordDigest(adminPass);
		service.updateUser(admin);
	}

	/**
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
		throws SAXException {
		if (namespaceURI.equals(NS)) {
			if (localName.equals("collection")) {
				final String name = atts.getValue("name");
				final String owner = atts.getValue("owner");
				final String group = atts.getValue("group");
				final String mode = atts.getValue("mode");
				final String created = atts.getValue("created");

				
				if (name == null)
					throw new SAXException("collection requires a name " + "attribute");
				try {
					if(dialog != null)
						dialog.displayMessage("creating collection " + name);
					
					
					
					Date date_created = null;
					
					if (created != null)
						try {
							date_created = (Date)(new DateTimeValue(created)).getDate();
						} catch (XPathException e2) {
						} 

					 
					
					current = mkcol(name, date_created);
					UserManagementService service =
						(UserManagementService) current.getService("UserManagementService", "1.0");
					User u = new User(owner, null, group);
					service.chown(u, group);
					service.chmod(Integer.parseInt(mode, 8));
				} catch (Exception e) {
                    if (dialog != null) {
                    showErrorMessage("An unrecoverable error occurred while restoring\ncollection '" + name + "'. " +
                            "Aborting restore!");
                    } else {
                        System.err.println("An unrecoverable error occurred while restoring\ncollection '" + name + "'. " +
                            "Aborting restore!");
                    }
                    e.printStackTrace();
					throw new SAXException(e.getMessage(), e);
				}
				if(dialog != null)
					dialog.setCollection(name);
			} else if (localName.equals("subcollection")) {
				
				 String name = atts.getValue("filename");
				
				if (name == null) name = atts.getValue("name");

				final String fname =
					contents.getParentFile().getAbsolutePath()
						+ File.separatorChar
						+ name
						+ File.separatorChar
						+ "__contents__.xml";
				final File f = new File(fname);
				if (f.exists() && f.canRead())
					stack.push(f);
				else
					System.err.println(f.getAbsolutePath() + " does not exist or is not readable.");
			} else if (localName.equals("resource")) {

				String type = atts.getValue("type");
				if(type == null)
					type ="XMLResource";
				final String name = atts.getValue("name");
				final String owner = atts.getValue("owner");
				final String group = atts.getValue("group");
				final String perms = atts.getValue("mode");
				
				String filename = atts.getValue("filename");
				final String mimetype = atts.getValue("mimetype");
				final String created = atts.getValue("created");
				final String modified = atts.getValue("modified");
				
				if (filename == null) filename = name;

				if (name == null) {
                    if (dialog != null)
                        dialog.displayMessage("Wrong entry in backup descriptor: resource requires a name attribute.");
                    else
                        System.err.println("Wrong entry in backup descriptor: resource requires a name attribute.");
                }
				final File f =
					new File(
						contents.getParentFile().getAbsolutePath() + File.separatorChar + filename);
				try {
					if (dialog != null && current instanceof Observable) {
						((Observable) current).addObserver(dialog.getObserver());
					}
					if(dialog != null)
						dialog.setResource(name);
					final Resource res =
						current.createResource(name, type);
					if (mimetype != null)
						((EXistResource)res).setMimeType(mimetype);
					
					res.setContent(f);
					if(dialog == null)
						System.out.println("Restoring " + name);
					
					Date date_created = null;
					Date date_modified = null;
					
					if (created != null)
						try {
							date_created = (Date)(new DateTimeValue(created)).getDate();
						} catch (XPathException e2) {
                            System.err.println("Illegal creation date. Skipping ...");
						} 
					
					if (modified != null)
						try {
							date_modified = (Date)(new DateTimeValue(modified)).getDate();
						} catch (XPathException e2) {
                            System.err.println("Illegal modification date. Skipping ...");
						} 
					
					current.storeResource(res, date_created, date_modified);
					UserManagementService service =
						(UserManagementService) current.getService("UserManagementService", "1.0");
					User u = new User(owner, null, group);
					try {
						service.chown(res, u, group);
					} catch (XMLDBException e1) {
						if(dialog != null) {
							dialog.displayMessage("Failed to change owner on document '" + name + "'; skipping ...");
                        }
					}
					service.chmod(res, Integer.parseInt(perms, 8));
					if(dialog != null)
						dialog.displayMessage("restored " + name);
					
					/* if we restored /db/system/users.xml, the admin password
					 * may have changed, so we will get a permission denied
					 * exception when continuing with the restore. We thus set
					 * the password back to the old one and change it to the 
					 * new one at the very end of the restore.
					 */
					if (SecurityManager.SYSTEM.equals(current.getName()) && 
							SecurityManager.ACL_FILE.equals(name) &&
							SecurityManager.DBA_USER.equals(username)) {
						User admin = service.getUser(SecurityManager.DBA_USER);
						adminPass = admin.getPassword();
						admin.setPassword(pass);
						try {
							service.updateUser(admin);
						} catch (XMLDBException e) {
						}
					}
				} catch (Exception e) {
                    if (dialog != null) { 
                            dialog.displayMessage("Failed to restore resource '" + name + "'\nfrom file '" +
                                    f.getAbsolutePath() + "'.\nReason: " + e.getMessage());
                            showErrorMessage(
                                    "Failed to restore resource '" + name + "' from file: '" +
                                    f.getAbsolutePath() + "'.\n\nReason: " + e.getMessage()
                            );
                    } else {
                        System.err.println("Failed to restore resource '" + name + "' from file '" +
					        f.getAbsolutePath() + "'");
                        e.printStackTrace();
                    }
				}
			}
		}
	}

	private final CollectionImpl mkcol(String collPath, Date created) throws XMLDBException {
		if (collPath.startsWith("/db"))
			collPath = collPath.substring("/db".length());
		CollectionManagementServiceImpl mgtService;
		Collection c;
		Collection current = DatabaseManager.getCollection(uri + "/db", username, pass);
		String p = "/db", token;
		StringTokenizer tok = new StringTokenizer(collPath, "/");
		while (tok.hasMoreTokens()) {
			token = tok.nextToken();
			p = p + '/' + token;
			c = DatabaseManager.getCollection(uri + p, username, pass);
			if (c == null) {
				mgtService =
					(CollectionManagementServiceImpl) current.getService(
						"CollectionManagementService",
						"1.0");
				//current = mgtService.createCollection(token);
				current = mgtService.createCollection(token, created);
			} else
				current = c;
		}
		return (CollectionImpl)current;
	}
    
	public static void showErrorMessage(String message) {
        JTextArea msgArea = new JTextArea(message);
        msgArea.setEditable(false);
        msgArea.setBackground(null);
        JScrollPane scroll = new JScrollPane(msgArea);
        JOptionPane optionPane = new JOptionPane();
        optionPane.setMessage(new Object[]{scroll});
        optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        JDialog dialog = optionPane.createDialog(null, "Error");
        dialog.setResizable(true);
        dialog.pack();
        dialog.setVisible(true);
        return;
    }
}
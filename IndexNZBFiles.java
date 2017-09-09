/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.ArrayList;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.nntp.NNTPClient;
import org.apache.commons.net.nntp.NewsgroupInfo;
import org.apache.commons.net.nntp.Article;

/**
 * program that will find binary posts with nzb files and save the info to a database table
 */
public class IndexNZBFiles {

    public static void main(String[] args) throws SocketException, IOException {

        if (args.length != 7 ) {
            System.out.println("Usage: DownloadNZBFiles <hostname> <groupname> <article specifier> <newshost user> <newshost password> <dbuserid> <dbpassword>");
            return;
        }

        String hostname = args[0];
        String newsgroup = args[1];

        // Article specifier can be numeric or Id in form <m.n.o.x@host>
        //String articleSpec = args.length >= 3 ? args[2] : null;
        String articleSpec = args[2];

        NNTPClient client = new NNTPClient();
        client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.err), true));
        client.connect(hostname);

	// news server authentication
        String user = args[3];
        String password = args[4];
        if(!client.authenticate(user, password)) {
            System.out.println("Authentication failed for user " + user + "!");
            System.exit(1);
        }

        // mysql connection
        Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        
        String url = "jdbc:mysql://192.168.1.33:3306/nzb";

        String dbUser = args[5];
        String dbPassword = args[6];
        try {
        	con = DriverManager.getConnection(url, dbUser, dbPassword);
        } catch (SQLException ex) {
		System.out.println(ex.getLocalizedMessage());
		System.out.println("Authentication failed for dbuser " + dbUser + "!");
		System.exit(1);
        }

	// get newsgroup info
	// grab this many articles at a time
	long maxArticles = 100000;

        NewsgroupInfo group = new NewsgroupInfo();
        client.selectNewsgroup(newsgroup, group);

		System.out.println("Group Name: " + group.getNewsgroup() 
			+ "\n" 
			+ "Total Articles: " +  group.getArticleCountLong()
			+ "\n" 
			+ "First Article: " + group.getFirstArticleLong()
			+ "\n " 
			+ "Last Article: " + group.getLastArticleLong()
			);

		ArrayList<Long> articleNumbers = new ArrayList<Long>();
		ArrayList<String> filenames = new ArrayList<String>();

		// starting point will be the article number in articleSpec
		long startingPoint = Long.parseLong(articleSpec);


		// first available article in the group
		long groupFirstArticle = group.getFirstArticleLong();

		// last available article in the group
		long groupLastArticle = group.getLastArticleLong();


		// setting some working variables as 'cursors' that point to the set of
		// articles that we are working on right now
		long firstWorkingArticle = groupFirstArticle;
		long lastWorkingArticle = groupLastArticle;

		if (firstWorkingArticle < startingPoint) firstWorkingArticle = startingPoint;

		lastWorkingArticle = firstWorkingArticle + maxArticles;
	
		// we can only go to the last available article, 
		// so if the last working article is set greater than that just move it
		// to the last available article in the group
		if (lastWorkingArticle > groupLastArticle) lastWorkingArticle = groupLastArticle;

		long lastArticleRead = 0;

		while (lastArticleRead < groupLastArticle) {

			filenames.clear();
			articleNumbers.clear();

			BufferedReader brHdr;
			String line;
			
			// pull the headers into the buffered reader
			brHdr = (BufferedReader) client.retrieveArticleInfo(firstWorkingArticle, lastWorkingArticle);
			//brHdr = (BufferedReader) client.retrieveHeader("Subject", firstArticle, lastArticle);
			//brHdr = (BufferedReader) client.retrieveArticleHeader(i);
			
			if (brHdr != null) {
			    while((line=brHdr.readLine()) != null) {
				    	// header is tab delimited
					// article number is the first item
					// subject is the second item
			 		long  articleNumber = Long.parseLong(line.split("\t")[0]);
			 		String subject = line.split("\t")[1];

					lastArticleRead = articleNumber;
					
			 		String fromName = line.split("\t")[2];
			 		String msgDate = line.split("\t")[3];
			 		String messageId = line.split("\t")[4];
			 		messageId = messageId.substring(1);
			 		messageId = messageId.substring(0, messageId.length()-1);
			 		
			 		
			 		String strMessageSize = line.split("\t")[6];
			 		int messageSize = 0;
			 		try {
				 		messageSize = Integer.parseInt(strMessageSize);
			 		} catch (NumberFormatException nfe) {
			 		}
			 		
			 		
			 		String xref = line.split("\t")[8];
			 		xref = xref.substring(6);



					int pos = 0;
					// find ".nzb" in subject and remove it
					pos = subject.indexOf(".nzb");
					if (pos >= 0) {
						System.out.println(line );
						System.out.println();
						System.out.println("Article number: " + articleNumber + " Subject --->" + subject + "<---");
						System.out.println("Message size: " + messageSize );
						System.out.println();
						System.out.println();

			 			
					}

/**********************
					while (pos >= 0) {
						System.out.println("Removing \".nzb\" from subject --->" + subject + "<---");
						
						String leftSide = subject.substring(0, pos);
						String rightSide = subject.substring(pos +6);
						subject = leftSide + rightSide;
						System.out.println("New subject --->" + subject + "<---");

						pos = subject.indexOf("\".nzb\"");
					}


			 		if (
							(subject.indexOf(".nzb ") >= 0 || subject.indexOf(".nzb\"") >= 0 ) 
							) {
						System.out.println("Found subject with nzb: " + subject);

			 			String from = line.split("\t")[2];
			 			String msgDate = line.split("\t")[3];
						
						Date testDate = new Date();						
			    			SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss z");
			    			try {
							System.out.println("Formatting date: " + msgDate.toString());
			    				testDate = formatter.parse(msgDate);
					    		System.out.println("Article Date: " + testDate.toString());
			    			} catch (Exception e) {
			    				System.out.println(e.toString());
			    			}

			 			String messageId = line.split("\t")[4];
			 			String messageSize = line.split("\t")[6];
						String filename = "";
			
			 			int partNumber = 0;
			 			int totalParts = 0;
			 			pos = subject.lastIndexOf("(");

			    			try {
			 				partNumber = Integer.parseInt(subject.substring(pos+1).split("/")[0]);
			 			} catch (Exception e) {
			 				partNumber = -1;
			 			}
			    			try {
			 				totalParts = Integer.parseInt(subject.substring(pos+1, subject.length()-1).split("/")[1]);
			 			} catch (Exception e) {
			 				totalParts  = -1;
			 			}

			 			if (partNumber > 0 && totalParts > 0) {
			 				System.out.println("Looking at post with subject--->" + subject + "<--- ...");
							System.out.println("Figuring out the filename. Starting with --->" + filename + "<---");


			 				pos = subject.indexOf(".nzb");
							filename = subject.substring(0, pos);

							pos = filename.lastIndexOf("\"");
							if (pos >=0) {
								filename = filename.substring(pos+1);
							}

							filename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");

							if (filename.toString().trim().equals("") || filename.trim().equals(".nzb")) {
								System.err.println("Bad filename for " + subject.toString());
								// try again and just use the subject totally
								filename = subject.toString();
								filename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
							}

							//filename = filename + "_" + articleNumber + ".nzb";

			 				System.out.println("Adding file: -->" + filename + "<-- to the queue...");
			 				articleNumbers.add(articleNumber);
			 				filenames.add(filename);
			 			}
			
			
			
			 		}
					*/
			    }
			}
			brHdr.close();

			for (int i=0; i< articleNumbers.size(); i++) {
				System.out.println("Article Number: " + articleNumbers.get(i));
				line = "";
				brHdr = null;
				try {
					brHdr = (BufferedReader) client.retrieveArticleHeader(articleNumbers.get(i));
				} catch (Exception e) {
			    		System.out.println(e.toString());
			    	}

				String sender = "";
				int bytesSent = 0;

				System.out.println("####  Article Header Info ####");
				if (brHdr != null) {
				    while((line=brHdr.readLine()) != null) {
				        System.out.println(line);
					if (line.indexOf("Sender: ") == 0) {
						sender = line.substring(8);
					}
					if (line.indexOf("Bytes: ") == 0) {
						bytesSent = Integer.parseInt(line.substring(7));
					}
				    }
				    brHdr.close();
				}
				if (sender.equals("yEncBin@Poster.com")) {
					System.out.println("Probably should ignore this post: sender => " + sender + " and Bytes => " + bytesSent);
				}

				System.out.println("Retrieving article body....");
				Reader reader = client.retrieveArticleBody( articleNumbers.get(i));
				if (reader != null) {
					System.out.println("Writing temp.nzb file for " + filenames.get(i) + ".nzb");
					try {
						//FileOutputStream fos = new FileOutputStream( filenames.get(i) );
						FileOutputStream fos = new FileOutputStream( "temp.nzb" );
						char [] buffer = new char[512];
						int charsRead = reader.read(buffer);
						while (charsRead > -1 ) {
							fos.write(Charset.forName("ISO-8859-1").encode(CharBuffer.wrap(buffer, 0, charsRead)).array());
							charsRead = reader.read(buffer);
						}
						reader.close();

						fos.flush();
						fos.close();
			    		} catch (Exception e) {
			    			System.out.println(e.toString());
			    		}
				}

				try {
					String fullFilename = "output/" + filenames.get(i) + "/" + filenames.get(i) + "_" + articleNumbers.get(i) +  ".nzb";
					try{
						String strManyDirectories= "output/" + filenames.get(i);
						boolean success;

						// Create multiple directories
						success = (new File(strManyDirectories)).mkdirs();
						if (success) {
						  System.out.println("Directories: " + strManyDirectories + " created");
						}
						
					}catch (Exception e){//Catch exception if any
						  System.err.println("Error: " + e.getMessage());
					}  


					File f = new File( fullFilename );
					if (f.isFile()) {
					} else {
						// decode yenc encoded file
						//decode (new RandomAccessFile(filenames.get(i),"r"), "output/" + filenames.get(i));
						decode (new RandomAccessFile("temp.nzb","r"), fullFilename);
						// delete yenc encoded file since we are done with it
						//f = new File(filenames.get(i));
						//if (f.isFile()) {
						//	System.out.println("Delete " + filenames.get(i));
						//	f.delete();
						//}
					}
			    	} catch (Exception e) {
			    		System.out.println(e.toString());
			    	}

		/*	
				File file = new File(filenames.get(i));
				if (! file.exists()) file.createNewFile();

				Writer writer = new OutputStreamWriter( 
						new FileOutputStream(file.getAbsoluteFile()) , "UTF-8");

				BufferedWriter bw = new BufferedWriter(writer);

				BufferedReader brBody;
				brBody = (BufferedReader) client.retrieveArticleBody(articleNumbers.get(i));
			
				if (brBody != null) {
					line = "";
					while(( line = brBody.readLine()) != null) {
						//bw.write(line, 0, line.length());
						bw.write(line);
						bw.newLine();
				    }
				    brBody.close();
				}
				bw.close();
				*/ 
				
				/*

                                BufferedReader brBody;
                                brBody = (BufferedReader) client.retrieveArticleBody(articleNumbers.get(i));

                                if (brBody != null) {
                                        int value = 0;
                                        while(( value = brBody.read()) != -1) {
                                                bw.write(value);
                                    }
                                    brBody.close();
                                }
                                bw.close();
				


				*/
			}
			firstWorkingArticle = lastArticleRead + 1;

			lastWorkingArticle = firstWorkingArticle + maxArticles;
	
			// for now just grab maxArticles articles
			if (lastWorkingArticle > groupLastArticle) lastWorkingArticle = groupLastArticle;
		}
	}

    /**
     * This method does all of the decoding work.
     *
     * @param file   takes a file to read from
     * @param folder destination folder.
     *               File will be created based on the name provided by the header.
     *
     *               if there is an error in the header and the name
     *               can not be obtained, "unknown" is used.
     * @exception IOException
     */
    public static void decode(RandomAccessFile file, String folder) throws IOException{

      /* Get initial parameters */
      String line = file.readLine();
      while (line!=null && !line.startsWith("=ybegin")) {
        line = file.readLine();
      }
      if (line==null)
        return;

      String fileName = parseForName(line, "name");
      if (fileName==null)
          fileName = "Unknown.blob";
      fileName = folder + fileName;
      fileName = folder;

      RandomAccessFile fileOut = new RandomAccessFile(fileName, "rw");

      String partNo = parseForName(line, "part");

      /* Handle Multi-part */
      if (partNo!=null) {
          while (line!=null && !line.startsWith("=ypart")) {
            line = file.readLine();
          }
          if (line==null)
            return;

          /* Get part-related parameters */
          long begin = Long.parseLong(parseForName(line, "begin")) - 1;
          if (fileOut.length()<begin)
            fileOut.setLength(begin-1); // reset file
          fileOut.seek(begin);
      } else {
          fileOut.setLength(0); // reset file
      }

      /* Decode the file */
      int character;
      boolean special=false;

      line = file.readLine();
      while (line!=null && !line.startsWith("=yend")) {
          for (int lcv=0;lcv<line.length(); lcv++){
            character = (int)line.charAt(lcv);
            if (character != 61) {
                character = decodeChar(character, special);
                fileOut.write(character);
                //System.out.print((char) character);
                special = false;
            } else
                special = true;
          }
          line = file.readLine();
      }
      fileOut.close();
    }

    private static int decodeChar(int character, boolean special) throws IOException {
        int result;
        if (special)
          character = character-64;

        result = character-42;

        if (result<0)
          result += 256;

        return result;
    }

    private static String parseForName (String line, String param) {
        int indexStart = line.indexOf(param+"=");
        int indexEnd = line.indexOf(" ", indexStart);
        if (indexEnd==-1)
          indexEnd = line.length() ;
        if (indexStart>-1)
          return line.substring(indexStart+param.length()+1, indexEnd);
        else
          return null;
    }

    /**
     * Provides a way to find out which version this decoding engine is up to.
     *
     * @return Version number
     */
    public static int getVersionNumber(){
        return 2;
    }
}

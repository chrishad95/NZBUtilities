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

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.nntp.NNTPClient;
import org.apache.commons.net.nntp.NewsgroupInfo;
import org.apache.commons.net.nntp.Article;

/**
 * Sample program demonstrating the use of article header and body retrieval
 */
public class GetNZBFiles {

    public static void main(String[] args) throws SocketException, IOException {

        if (args.length != 2 && args.length != 3 && args.length != 5) {
            System.out.println("Usage: MessageThreading <hostname> <groupname> [<article specifier> [<user> <password>]]");
            return;
        }

        String hostname = args[0];
        String newsgroup = args[1];
        // Article specifier can be numeric or Id in form <m.n.o.x@host>
        String articleSpec = args.length >= 3 ? args[2] : null;

        NNTPClient client = new NNTPClient();
        client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.err), true));
        client.connect(hostname);

        if (args.length == 5) { // Optional auth
            String user = args[3];
            String password = args[4];
            if(!client.authenticate(user, password)) {
                System.out.println("Authentication failed for user " + user + "!");
                System.exit(1);
            }
        }

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

		long startingPoint = Long.parseLong(articleSpec);

		// grab this many articles at a time
		long maxArticles = 100000;

		long groupFirstArticle = group.getFirstArticleLong();
		long groupLastArticle = group.getLastArticleLong();

		long firstArticle = groupFirstArticle;
		long lastArticle = groupLastArticle;

		if (firstArticle < startingPoint) firstArticle = startingPoint;

		lastArticle = firstArticle + maxArticles;
	
		// for now just grab maxArticles articles
		if (lastArticle > groupLastArticle) lastArticle = groupLastArticle;
		long lastArticleRead = 0;

		while (lastArticleRead < groupLastArticle) {

			filenames.clear();
			articleNumbers.clear();

			BufferedReader brHdr;
			String line;
			
			brHdr = (BufferedReader) client.retrieveArticleInfo(firstArticle, lastArticle);
			//brHdr = (BufferedReader) client.retrieveHeader("Subject", firstArticle, lastArticle);
			//brHdr = (BufferedReader) client.retrieveArticleHeader(i);
			
			if (brHdr != null) {
			    while((line=brHdr.readLine()) != null) {
			 		String subject = line.split("\t")[1];

			 		long  articleNumber = Long.parseLong(line.split("\t")[0]);
					lastArticleRead = articleNumber;

			 		if (subject.indexOf(".nzb") >= 0) {
						System.out.println("Found subject with nzb: " + subject);

			 			String from = line.split("\t")[2];
			 			String msgDate = line.split("\t")[3];
						
						Date testDate = new Date();						
			    			SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss z");
			    			try {
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
			 			int pos = subject.lastIndexOf("(");
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
			 				System.out.println("Looking at post with subject: " + subject + " to the queue...");
			 				articleNumbers.add(articleNumber);
			    				filename = subject;

			 				pos = subject.lastIndexOf("\"");
							int quotes = 0;
							if (pos >= 0) {
								// there is a quote at the end of the filename
								filename = subject.substring(0, pos);
								System.out.println("Filename has quotes around it. Starting with " + filename);
								quotes = 1;
							} else {
			 					pos = subject.indexOf(".nzb");
								filename = subject.substring(0, pos+4);
								System.out.println("Filename does not have quotes around it. Starting with " + filename);
							}
							
							if (quotes == 1) {
								System.out.println("Filename has quotes around it. Finding beginning of filename");
			 					while (filename.indexOf("\"") >=0) {
			 						pos = filename.indexOf("\"");
			 						filename = filename.substring(pos+1);
									System.out.println("Filename has quotes around it. Cutting filename down to " + filename);
			 					}
							} else {
								// no quotes so look for the last space before .nzb
			 					while (filename.indexOf(" ") >=0) {
			 						pos = filename.indexOf(" ");
			 						filename = filename.substring(pos+1);
									System.out.println("Filename does not have quotes around it. Cut filename down to " + filename);
			 					}
							}
			 				System.out.println("Adding file: " + filename + " to the queue...");
			 				filenames.add(filename);
			 			}
			
			     		/*System.out.println("\nSubject: " + subject + " " + msgDate);
			 			System.out.println("Filename: " + filename);
			     		System.out.println("Article Number: " + articleNumber);
			     		System.out.println("From: " + from);
			     		System.out.println("Date: " + msgDate);
			     		System.out.println("Message ID: " + messageId);
			     		System.out.println("Size: " + messageSize);
			 			System.out.println("Part Number:" + partNumber);
			 			System.out.println("Total Parts:" + totalParts);
						*/
			
			
			 		}
			    }
			}
			brHdr.close();

			for (int i=0; i< articleNumbers.size(); i++) {
				System.out.println(articleNumbers.get(i));
				line = "";
				brHdr = (BufferedReader) client.retrieveArticleHeader(articleNumbers.get(i));
			
				if (brHdr != null) {
				    while((line=brHdr.readLine()) != null) {
				        System.out.println(line);
				    }
				    brHdr.close();
				}

				Reader reader = client.retrieveArticleBody( articleNumbers.get(i));
				if (reader != null) {
					System.out.println("Writing file: " + filenames.get(i));

					FileOutputStream fos = new FileOutputStream( filenames.get(i) );
					char [] buffer = new char[512];
					int charsRead = reader.read(buffer);
					while (charsRead > -1 ) {
						fos.write(Charset.forName("ISO-8859-1").encode(CharBuffer.wrap(buffer, 0, charsRead)).array());
						charsRead = reader.read(buffer);
					}
					reader.close();
					fos.flush();
					fos.close();
				}
				decode (new RandomAccessFile(filenames.get(i),"r"), "output/" + filenames.get(i));

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
			firstArticle = lastArticleRead + 1;

			lastArticle = firstArticle + maxArticles;
	
			// for now just grab maxArticles articles
			if (lastArticle > groupLastArticle) lastArticle = groupLastArticle;
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

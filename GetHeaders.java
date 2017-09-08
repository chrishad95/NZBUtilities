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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.nntp.NNTPClient;
import org.apache.commons.net.nntp.NewsgroupInfo;
import org.apache.commons.net.nntp.Article;

/**
 * Sample program demonstrating the use of article header and body retrieval
 */
public class GetHeaders {

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
        client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
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

		// get the last 10 headers for now
		
		long startingPoint = Long.parseLong(articleSpec);

		long firstArticle = group.getFirstArticleLong();
	       	if (startingPoint > firstArticle) { 
			firstArticle = startingPoint;
		}
		long lastArticle = group.getLastArticleLong();
		
		long maxArticles = 10;
		// for now just grab maxArticles articles
		if (lastArticle > firstArticle + maxArticles) {
			lastArticle = firstArticle + maxArticles;
		}

//		Iterable<Article> articles = client.iterateArticleInfo(firstArticle, lastArticle);
//		
//		for (Article article: articles) {
//
//		String subject = article.getSubject();
//
//		int partNumber = 0;
//		int totalParts = 0;
//		int pos = subject.lastIndexOf("(");
//		partNumber = Integer.parseInt(subject.substring(pos+1).split("/")[0]);
//		totalParts = Integer.parseInt(subject.substring(pos+1, subject.length()-1).split("/")[1]);
//		System.out.println("Part Number:" + partNumber);
//		System.out.println("Total Parts:" + totalParts);
//
//        //	System.out.println("Headers....\n\n\n");
//		System.out.println("       Subject: " + article.getSubject());
//		System.out.println("          Date: " + article.getDate());
//		System.out.println("          From: " + article.getFrom());
//		System.out.println("    Article ID: " + article.getArticleId());
//		System.out.println("Article Number: " + article.getArticleNumberLong());
//		}

				
       BufferedReader brHdr;
       String line;

       brHdr = (BufferedReader) client.retrieveArticleInfo(firstArticle, lastArticle);
       //brHdr = (BufferedReader) client.retrieveHeader("Subject", firstArticle, lastArticle);
       //brHdr = (BufferedReader) client.retrieveArticleHeader(i);

       System.out.println("Headers....\n\n\n");
       if (brHdr != null) {
           while((line=brHdr.readLine()) != null) {
		   		String articleNumber = line.split("\t")[0];
				String subject = line.split("\t")[1];
				String from = line.split("\t")[2];
				String msgDate = line.split("\t")[3];
				String messageId = line.split("\t")[4];
				String messageSize = line.split("\t")[6];

				int partNumber = 0;
				int totalParts = 0;
				int pos = subject.lastIndexOf("(");
				partNumber = Integer.parseInt(subject.substring(pos+1).split("/")[0]);
				totalParts = Integer.parseInt(subject.substring(pos+1, subject.length()-1).split("/")[1]);
		   	
               System.out.println("\nSubject: " + subject);
               System.out.println("Article Number: " + articleNumber);
               System.out.println("From: " + from);
               System.out.println("Date: " + msgDate);
               System.out.println("Message ID: " + messageId);
               System.out.println("Size: " + messageSize);
				System.out.println("Part Number:" + partNumber);
				System.out.println("Total Parts:" + totalParts);
               System.out.println("--->" + line);
           }
           brHdr.close();
       }


    }

}

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

import java.sql.Connection;
import java.util.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
/**
 * Sample program demonstrating the use of article header and body retrieval
 */
public class ListNewsgroups {

    public static void main(String[] args) throws SocketException, IOException {

        if (args.length != 3 && args.length != 5) {
            System.out.println("Usage: ListNewsgroups <hostname> [<user> <password>]");
            return;
        }

        String hostname = args[0];

        NNTPClient client = new NNTPClient();
        client.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
        client.connect(hostname);
		if (client.isConnected() ) {
			System.out.println("Connected to " + hostname );
		}

        if (args.length >= 3) { // Optional auth
            String user = args[1];
            String password = args[2];
            if(client.authenticate(user, password)) {
                System.out.println("Authentication was successful  for user " + user + "!");
			} else {
                System.out.println("Authentication failed for user " + user + "!");
                System.exit(1);
            }
        }

        // mysql connection
        Connection con = null;
        
        PreparedStatement pst = null;
        ResultSet rs = null;
        
        String url = "jdbc:mysql://192.168.1.33:3306/nzb";

        String dbUser = "";
        String dbPassword = "";
	if (args.length >= 5) {
		dbUser = args[3];
		dbPassword = args[4];
	}

        try {
        	con = DriverManager.getConnection(url, dbUser, dbPassword);

        	pst = con.prepareStatement("select ID from group_info WHERE NAME = ?");
		PreparedStatement updateGroupInfoStmt = 
			con.prepareStatement("update group_info set article_count=?, first_article=?, last_article=? where id=?");
		PreparedStatement insertGroupInfoStmt = 
			con.prepareStatement("insert into group_info (article_count, first_article, last_article, name)"
				+ " values (?,?,?,?)");

        	int groupID = -1;

		NewsgroupInfo[] groups;
		groups = client.listNewsgroups();
		if (groups != null) {
			for (int i=0; i< groups.length; i++) {

				System.out.println(groups[i].getNewsgroup() 
					+ ", " 
					+ groups[i].getArticleCountLong()
					+ ", " 
					+ groups[i].getFirstArticleLong()
					+ ", " 
					+ groups[i].getLastArticleLong()
					);

				long groupArticleCount = groups[i].getArticleCountLong();
				long groupLastArticle = groups[i].getLastArticleLong();
				long groupFirstArticle = groups[i].getFirstArticleLong();
				String groupName = groups[i].getNewsgroup();

        			pst.setString(1, groupName);
        			
        			rs = pst.executeQuery();
				
				// check to see if there is a record in group_info for this group
        			if (rs.next()) {
        				groupID = rs.getInt(1);

        				//lastRecord = rs.getLong(2);
				        updateGroupInfoStmt.setLong(1, groupArticleCount);
				        updateGroupInfoStmt.setLong(2, groupFirstArticle);
				        updateGroupInfoStmt.setLong(3, groupLastArticle);
				        updateGroupInfoStmt.setInt(4, groupID);
				        updateGroupInfoStmt.execute();
        		
        			} else {
					// need to create the record
				        insertGroupInfoStmt.setLong(1, groupArticleCount);
				        insertGroupInfoStmt.setLong(2, groupFirstArticle);
				        insertGroupInfoStmt.setLong(3, groupLastArticle);
				        insertGroupInfoStmt.setString(4, groupName);
				        insertGroupInfoStmt.execute();
        			}

			}
		} else {
			System.err.println("LIST command failed.");
			System.err.println("Server reply: " + client.getReplyString());
		}
        	if (rs != null) {
        		rs.close();
        	}
        	if (pst != null) {
        		pst.close();
        	}

        } catch (SQLException ex) {
        	System.out.println(ex.getLocalizedMessage());
        } finally {
        	try {
        		if (pst != null) {
        			pst.close();
        		}
        		if (con != null) {
        			con.close();
        		}
        	} catch (SQLException ex) {
        		System.out.println(ex.getMessage());
        	}
        }

    }

    public static void updateGroup(String name, long article_count, long first_article, long last_article) {


    }

}

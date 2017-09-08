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

/**
 * Sample program demonstrating the use of article header and body retrieval
 */
public class ListNewsgroups {

    public static void main(String[] args) throws SocketException, IOException {

        if (args.length != 3) {
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

        if (args.length == 3) { // Optional auth
            String user = args[1];
            String password = args[2];
            if(client.authenticate(user, password)) {
                System.out.println("Authentication was successful  for user " + user + "!");
			} else {
                System.out.println("Authentication failed for user " + user + "!");
                System.exit(1);
            }
        }

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
			}
		} else {
			System.err.println("LIST command failed.");
			System.err.println("Server reply: " + client.getReplyString());
		}

    }

}

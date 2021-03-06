package edu.vu.groupcast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.logging.Logger;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class ClientConnection extends Thread {

	private static final Logger LOG = Logger.getLogger(Thread.currentThread()
			.getStackTrace()[0].getClassName());

	private static final int STATUS_ERROR = 0;
	private static final int STATUS_OK = 1;
	private static final int MSG = 2;

	Server server;
	Socket cs;
	String name;
	boolean running;
	PrintStream out;
	BufferedReader in;
    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;



	public ClientConnection(Server server, Socket cs) throws IOException {
		LOG.info("client connection instantiated");
		this.server = server;
		this.cs = cs;
		this.in = new BufferedReader(new InputStreamReader(cs.getInputStream()));
		this.out = new PrintStream(cs.getOutputStream());
		this.running = false;
	}

	synchronized public void sendMsg(int type, String msg) {
		if (type == STATUS_OK)
			out.println("+OK," + msg);
		else if (type == STATUS_ERROR)
			out.println("+ERROR," + msg);
		else if (type == MSG)
			out.println("+MSG," + msg);
	}

	@Override
	public void run() {
		//get connection to database
	    try {
	        conn =
	           DriverManager.getConnection("jdbc:mysql://messagedb.czvzkcvchbns.us-west-2.rds.amazonaws.com:2000/mydb?" +
	                                       "user=russelan&password=kt121nbn");
		    
	        String sql = "CREATE TABLE messages " +
	                "(time TIMESTAMP, " +
	                " from STRING, " + 
	                " message STRING, " + 
	                " group STRING, " + 
	                " PRIMARY KEY ( time ))";
		    stmt = conn.createStatement();
		    stmt.executeUpdate(sql);
		    LOG.info("connected to db and table created");
	    }
	    catch (SQLException ex) {
	        System.out.println("SQLException: " + ex.getMessage());
	        System.out.println("SQLState: " + ex.getSQLState());
	        System.out.println("VendorError: " + ex.getErrorCode());
	    }
	    
		running = true;
		
		try {
//			sendMsg(STATUS_OK, server.toString());

			while (running) {

				LOG.info(this.cs.getRemoteSocketAddress().toString() + ": Waiting for input from client");
				String msg = in.readLine();
							
				if(msg == null) { // client closed the connection
					break;
				}
				
				// String[] tokens = msg.trim().split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
				String[] tokens = msg.split(",");

				
				StringBuilder sb1 = new StringBuilder();
								for (String t : tokens) {
					sb1.append(t);
					sb1.append(',');
				}
				
				if(sb1.length()>0)
					sb1.deleteCharAt(sb1.length()-1); // remove last comma
				LOG.info(this.cs.getRemoteSocketAddress().toString() + ": read :" + sb1.toString());

				// check if there's a valid command
				if (tokens.length == 0) {
					sendMsg(STATUS_ERROR, "No command given");
				} else {

					String cmd = tokens[0].trim();

					
					// handle BYE
					if ("BYE".equalsIgnoreCase(cmd)) {
						sendMsg(STATUS_OK, "BYE");
						break;
					}
					// handle VERSION
					else if ("VERSION".equalsIgnoreCase(cmd)) {
						sendMsg(STATUS_OK, "VERSION,"+server.toString());
					}

					// handle NAME
					else if ("NAME".equalsIgnoreCase(cmd)) {
						LOG.info("name set");
						if (this.name != null) {
							sendMsg(STATUS_ERROR, "NAME: already set");
						}

						else if (tokens.length < 2) {
							sendMsg(STATUS_ERROR, "NAME: not specified");

						} 
						else if(tokens[1].startsWith("@")) {
							sendMsg(STATUS_ERROR, "NAME: cannot start with @");							
						} else

							try {
								server.addClient(tokens[1], this);
								this.name = tokens[1];
								sendMsg(STATUS_OK, "NAME," + this.name);
							} catch (ClientNameException e) {
								sendMsg(STATUS_ERROR, "NAME,"+ tokens[1] +": already in use");
							}

					}

					// handle LIST
					else if ("LIST".equalsIgnoreCase(cmd)) {
						if (tokens.length < 2) {
							sendMsg(STATUS_ERROR, "LIST: parameter missing");							
						} else {
							if("USERS".equalsIgnoreCase(tokens[1])) {
								if(tokens.length == 2) {
									// list all users

									StringBuilder sb = new StringBuilder();
									synchronized(server.clients) {
									for(String clientName: server.clients.keySet()) {
										sb.append(clientName);
										sb.append(',');
									}
									}
									
									if(sb.length()>0)
										sb.deleteCharAt(sb.length()-1);
									sendMsg(STATUS_OK, "LIST,USERS:"+sb.toString());
									
								} else {
									// list users in specified group
									String groupName = tokens[2];
									Group g = server.getGroupByName(groupName);
									if(g == null) {
										sendMsg(STATUS_ERROR, "LIST,USERS,"+groupName+": group not found");
									} else {
										
										StringBuilder sb = new StringBuilder();
										synchronized(g.members) {
										for(ClientConnection member: g.members) {
											sb.append(member.name);
											sb.append(',');
										}
										}
										
										if(sb.length()>0)
											sb.deleteCharAt(sb.length()-1);
																	
										sendMsg(STATUS_OK, "LIST,USERS,"+groupName+':'+sb.toString());
									}
								}
							}
							else if("GROUPS".equalsIgnoreCase(tokens[1])) {
								// list groups

								StringBuilder sb = new StringBuilder();								
								synchronized(server.groups) {
								for(Group group: server.groups.values()) {
									sb.append(group.toString());
									sb.append(',');
								}
								}
								
								if(sb.length()>0)
									sb.deleteCharAt(sb.length()-1);
								sendMsg(STATUS_OK, "LIST,GROUPS:" + sb.toString());
																
							}
							else if("MYGROUPS".equalsIgnoreCase(tokens[1])) {
								// list my group memberships
								
								StringBuilder sb = new StringBuilder();
								synchronized(server.groups) {
								for(Group group: server.groups.values()) {
									synchronized(group.members) {
									if(group.members.contains(this)) {
										sb.append(group.toString());
										sb.append(',');
									}
									}
								}
								}
								
								if(sb.length()>0)
									sb.deleteCharAt(sb.length()-1);
								
								sendMsg(STATUS_OK, "LIST,MYGROUPS:"+sb.toString());
								
							}
							else {
								sendMsg(STATUS_ERROR, "LIST: Invalid parameter: "+tokens[1]);
							}
						}
					}

					// handle JOIN
					// if group didn't already exist, add it to db, members = client name
					// if it did exist, add client name to members
					else if ("JOIN".equalsIgnoreCase(cmd)) {
						LOG.info("received join");

						if (this.name == null) {
							sendMsg(STATUS_ERROR, "JOIN: name not set");
						}

						else if (tokens.length < 2) {
							sendMsg(STATUS_ERROR, "JOIN: no group given");
						}

						else if (!tokens[1].startsWith("@")) {
							sendMsg(STATUS_ERROR, "JOIN: group must start with @");
						}
						else {

							String groupName = tokens[1];

							try {
								int maxMembers = 0;
								if (tokens.length > 2) {
									maxMembers = Integer.parseInt(tokens[2]);
								}
								Group group = server.joinGroup(groupName, this, maxMembers);
								sendMsg(STATUS_OK, "JOIN," + group.toString());
							} catch (GroupFullException e) {
								sendMsg(STATUS_ERROR, "JOIN," + groupName
										+ ": group is full");
							} catch (NumberFormatException e) {
								sendMsg(STATUS_ERROR,
										"JOIN,"+groupName+": invalid maximum group size");
							} catch (MaxMembersMismatchException e) {
								sendMsg(STATUS_ERROR,
										"JOIN,"+groupName+": maximum group size mismatch with existing group");
							}
						}
					}

					// handle QUIT
					else if ("QUIT".equalsIgnoreCase(cmd)) {
						if (tokens.length < 2) {
							sendMsg(STATUS_ERROR, "QUIT: no group given");
						}

						else {
							String groupName = tokens[1];

							try {
								server.quitGroup(groupName, this);
								sendMsg(STATUS_OK, "QUIT," + groupName);
							} catch (NoSuchGroupException e) {
								sendMsg(STATUS_ERROR, "QUIT," + groupName
										+ ": group does not exist");
							} catch (NonMemberException e) {
								sendMsg(STATUS_ERROR, "QUIT," + groupName
										+ ": client is not a member");
							}

						}
					}

					// handle MSG
					// group name, members, list of messages in order
					// if only to one client group name = address
					else if ("MSG".equalsIgnoreCase(cmd)) {
						LOG.info("received msg");

						if (this.name == null) {
							sendMsg(STATUS_ERROR, "MSG: name not set");
						}

						else if (tokens.length < 2) {
							sendMsg(STATUS_ERROR, "MSG: no address given");
						}

						else if (tokens.length < 3) {
							sendMsg(STATUS_ERROR, "MSG: message body empty");
						}

						else {
							String address = tokens[1];
							
							StringBuilder sb = new StringBuilder();
							for(int i=2; i<tokens.length; i++) {
								sb.append(tokens[i]);
								sb.append(',');
							}
							
							if(sb.length()>0)
								sb.deleteCharAt(sb.length()-1);

							String body = sb.toString();
							
							try {
								java.util.Date date = new java.util.Date();
								Timestamp ts = new Timestamp(date.getTime());
								PreparedStatement ps = conn.prepareStatement("INSERT INTO `messages` (time,from,message,group)" +
															"VALUE (?,?,?,?)");
								ps.setTimestamp(1, ts );
								ps.setString(2, this.name);
								ps.setString(3, body);
								ps.setString(4, address);
								ps.executeUpdate();
							}
							catch (SQLException ex) {
							    // handle any errors
							    System.out.println("SQLException: " + ex.getMessage());
							    System.out.println("SQLState: " + ex.getSQLState());
							    System.out.println("VendorError: " + ex.getErrorCode());
							}

							HashSet<ClientConnection> dsts = new HashSet<ClientConnection>();
							Group g = server.getGroupByName(address);

							if (g != null) {
								LOG.info("Found group " + address + ": "
										+ g.toString());
								synchronized(g.members) {
									dsts.addAll(g.members);
								}
							} else {
								LOG.info("Group " + address + " not found");
							}

							ClientConnection client = server
									.getClientByName(address);
							if (client != null)
								dsts.add(client);

							dsts.remove(this);

							if (!dsts.isEmpty()) {
								int cnt = 0;
								for (ClientConnection c : dsts) {
										c.sendMsg(MSG, this.name + ","
											+ address + "," + body);
										
										if(!c.out.checkError()) {
											cnt++;
										} else {
											// error writing socket output stream: close it
											c.running = false;
											c.out.close();
											// this will implicitly remove the client and its singleton groups
										}
								}
								sendMsg(STATUS_OK,
										"MSG,"+address+","+body+": " + cnt
												+ " client(s) notified");
							} else {
								sendMsg(STATUS_ERROR, "MSG,"+address+","+body+": no recipients found");
							}

						}
					}
					
					// handle DB
					else if ("DB".equalsIgnoreCase(cmd)) {
						String group = tokens[1];
						String msgs = "";
						if (group != null) {
							try {
							    stmt = conn.createStatement();
							    String query = "SELECT * FROM `messages` WHERE group = `group`";
							    rs = stmt.executeQuery(query);
						        while (rs.next()) {
						            msgs = msgs + rs.getTimestamp("time")
						            + "," + rs.getString("from")
						            + "," + rs.getString("message")
						            + ",";
						        }
						        if  (msgs.length() > 0) {
						        	msgs = msgs.substring(0, msgs.length() - 2);
						        }
							}
							catch (SQLException ex) {
								// handle any errors
								System.out.println("SQLException: " + ex.getMessage());
								System.out.println("SQLState: " + ex.getSQLState());
								System.out.println("VendorError: " + ex.getErrorCode());
							}
						}
						sendMsg(STATUS_OK, msgs);
					}

					else {
						sendMsg(STATUS_ERROR, "Invalid command (" + cmd+")");
					}
				}
			}

		} catch (IOException e) {
			if(running) {
				// only print error if the client was supposed to be running, i.e. it wasn't stopped explicitly
				e.printStackTrace();
			}
		} finally {
			// make sure client is removed from server's data structures
			server.removeClient(this);
			
			try {
				in.close();
			} catch (IOException e) {}
			out.close();
			try {
				cs.close();
			} catch (IOException e) {}
			LOG.info("Client connection terminated");
		}

	}
}

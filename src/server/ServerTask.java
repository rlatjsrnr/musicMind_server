package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

import dao.MemberDAOImpl;
import dao.MemberVO;

public class ServerTask implements Runnable {
	/**
	 * main Socket
	 */
    private Socket clientSocket;
    
    /**
      방만들기 때 생성되는 서버소캣 
     */
    private ServerSocket roomServer;
    /**
     * main Client Reader
     */
    private BufferedReader clientReader;
    /**
     * main Client Writer
     */
    private PrintWriter clientWriter;

    private boolean isRun = true;
    
    /**
     * client가 사용중인 ID
     */
    private String userId;
    
    
    private MemberDAOImpl mDao;
	
    private MemberVO member;
    /**
     * 게임 방에 접속 되어있는 유저 리스트
     */
	private Hashtable<MemberVO, PrintWriter> roomUserList;
	
	public String roomName;

    public ServerTask(Socket socket) {
    	
        this.clientSocket = socket;
        try {
			InputStream is = clientSocket.getInputStream();
			OutputStream os = clientSocket.getOutputStream();
        	
        	clientWriter = new PrintWriter(os, true);
			clientReader = new BufferedReader(new InputStreamReader(is));
		} catch (IOException e) {
			ServerController.sc.printText("Client 연결 오류 : " + e.getMessage());
		}
    }

    @Override
    public void run() {
    	ServerController.sc.printText(clientSocket.getRemoteSocketAddress()+" receive 시작");
        try {
            while(isRun) {
    			try {
    				// client 메시지가 전달 될 때까지 blocking
    				String receiveData = clientReader.readLine();
    				if(receiveData == null) {
    					break;
    				}
    				String[] data = receiveData.split("\\|");
    				/**
    				  code
    				  "0" : 회원 정보
    				  "2" : 유저가 입력한 채팅 메시지
    				  "j" : 회원가입 요청
    				  "l" : 로그인 성공
    				  "z" : 로그인 실패
    				  "room" : 방 만들기 요청
    				  "roomName" : 생성된 방 정보를 테이블에 저장하고 클라이언트의 방 목록 갱신
    				  "roomEnter" : 입장하려는 방의 포트번호 요청
    				 */
    				String code = data[0];
    				String text = data[1];
    				switch(code) {
    				// 회원의 id와 pw를 전달 받아 로그인
    				case "0":
    					String[] info = text.split(","); 
    					mDao = new MemberDAOImpl();
    					// 데이터베이스에서 일치하는 정보 검색
    					MemberVO member = mDao.selectMember(info[0], info[1]);
    					if(member == null) {
    						this.clientWriter.println("z|계정이 존재하지않습니다.");
    					}else {
    						// 로그인 성공하면 아이디 저장
        					userId = info[0];
        					// client목록에 등록
        					ServerController.sc.clients.put(member, clientWriter);
        					this.member = member;
        					
        					// 로그인 성공 했다고 클라이언트에 알려줌
        					this.clientWriter.println("l|"+info[0]+","+info[1]);
        					// client 목록 갱신
        					String sendData = "";
        					for(MemberVO m : ServerController.sc.clients.keySet()) {
        						sendData += m.getMId()+","+m.getPoint();
        						sendData += " ";
        					}
        					broadCast("0", sendData);
        					
        					// 방 목록 갱신
        					if(ServerController.sc.roomList.size()>0) {
        						sendData = "";
                        		for(String s : ServerController.sc.roomList.keySet()) {
                        			sendData += s+" ";
                        		}
                        		broadCast("roomList", sendData);
        					}
    					}
    					break;
    					
    					// 채팅창에 표시될 메시지 전송
    				case "2":
    					String[] str = text.split(",");
    					broadCast("2", userId+":"+str[0]);
    					break;
    					
    					// 회원 가입 요청
    					// j|id,pw
    				case "j":
    					String[] info1 = text.split(",");
    					String id = info1[0];
    					String pw = info1[1];

    					boolean result = mDao.join(id, pw);
    					if(result) {
    						clientWriter.println("s|success");
    					}else {
    						clientWriter.println("f|fail");
    					}
    					break;
    					
    					// 방 생성 요청
    				case "room" :
    					createRoom();
    				    break;
	            		
	            	// 방에 입장하려는 클라이언트와 통신과정
	            	// text == roomName;
    				case "roomEnter":
    					this.roomName = text;
    					int port = ServerController.sc.roomList.get(text);
    					clientWriter.println("enterPort|"+port);
    				}
    				
    			}catch (IOException e) {
    				isRun = false;
    			}
    		}// while
            // 클라이언트와의 통신 작업 종료 후 소켓 및 리소스 해제
            clientWriter.close();
            clientReader.close();
            clientSocket.close();
        } catch (IOException e) {
            // 통신 중 오류 발생 시 예외 처리
        	ServerController.sc.printText("통신 오류: " + e.getMessage());
            isRun = false;
        }
        // while- task 작업 종료
 		// client 소켓도 종료
 		if(clientSocket != null && !clientSocket.isClosed()) {
 			try {
 				clientSocket.close();
 			} catch (IOException e) {}
 		}
 		ServerController.sc.clients.remove(this.member);
 		// 나간 인원 정보로 목록 갱신
		String sendData = "";
		for(MemberVO m : ServerController.sc.clients.keySet()) {
			sendData += m.getMId()+","+m.getPoint();
			sendData += " ";
		}
		broadCast("0", sendData);
 		broadCast("1", userId+"님이 나가셨습니다. 방인원 : "+ServerController.sc.clients.size());
    }
    
    /**
     * 게임 내 사용자들에게만 메시지 전달
     */
    public void roomBroadCast(String code, String msg) {
		for(PrintWriter p : roomUserList.values()) {
			p.println(code+"|"+msg);
		}
	}
    
    /**
     * 게임 내 사용자중 나를 제외하고 메시지 전달
     */
    public void roomBroadXY(String code, String msg) {
		for(PrintWriter p : roomUserList.values()) {
			if(p != ServerController.sc.roomWriter.get(this.roomName)) {
				p.println(code+"|"+msg);
			}
		}
	}
    
    /**
     * 대기실 내 모든 사용자에게 메시지 전달     
     */
    public void broadCast(String code, String msg) {
		for(PrintWriter p : ServerController.sc.clients.values()) {
			p.println(code+"|"+msg);
		}
	}
	/**
	 * 메시지를 보내온 클라이언트를 제외하고 다른 클라이언트들에 데이터 전달	 
	 */
	public void broadXY(String code, String msg) {
		for(PrintWriter p : ServerController.sc.clients.values()) {
			if(p != this.clientWriter) {
				p.println(code+"|"+msg);
			}
		}
	}
	
    // 방 만들기
    private void createRoom() {
    	// 게임 내 사용자를 저장할 공간 생성
    	roomUserList = new Hashtable<>();
    	
        try {
        	// 포트번호는 방이 생성될 때마다 1씩 증가시킴
            ServerController.roomPort++;
            roomServer = new ServerSocket(ServerController.roomPort);
            
            // 소캣이 생성되면 포트번호를 사용자에게 전송
            clientWriter.println("port|"+ServerController.roomPort);
            
            // 스레드 풀로 관리
            Runnable run = new Runnable() {
				@Override
				public void run() {
					try {
	                    while (true) {
	                    	// 클라이언트 접속 대기
	                        Socket client = roomServer.accept();
	                        new RoomServerTask(client, ServerTask.this);
	                    }
	                } catch (IOException e) {
	                    stopServer();
	                }
				}
            };
            ServerController.serverPool.submit(run);
            
        } catch (IOException e) {
        	ServerController.sc.printText("[방 생성 실패 : 포트 번호 " + ServerController.roomPort + "]");
        }
    }

    // 방 해산
    public void stopServer() {
		if(roomUserList != null) {
			for(PrintWriter p : roomUserList.values()) {
				if(p != null) {
					p.close();
				}
			}
		}
		roomUserList.clear();
		
		// roomServerSocket 종료
		try {
			if(roomServer != null && !roomServer.isClosed()) {
				roomServer.close();
			}
		} catch (IOException e) {}
	}
    
    public Hashtable<MemberVO, PrintWriter> getRoomUserList() {
		return roomUserList;
	}
    
	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}

}

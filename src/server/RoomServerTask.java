package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import dao.MemberVO;

public class RoomServerTask {
	/**
	 * 게임 방을 만들 때 생성되는 소캣
	 */
	private Socket gameRoomSocket;
	
	/**
	 * main server task
	 */
	private ServerTask masterTask;
	
	/**
	 * 게임 방 안에서 사용될 client reader
	 */
    private BufferedReader reader;
    /**
     * 게임 방 안에서 사용될 client writer
     */
    public PrintWriter writer;
    
    private boolean roomIsRun = true;
    
    /**
     * 연결된 client의 객체
     */
	private MemberVO roomMember;
	/**
	 * 현재 입장한 게임 방 이름
	 */
	private String roomName;
	
	public RoomServerTask(Socket gameRoomSocket, ServerTask serverTask) {
		this.gameRoomSocket = gameRoomSocket;
		this.masterTask = serverTask;
		handlegameRoomSocket();
	}
	
	private void handlegameRoomSocket() {
        // 클라이언트와 통신할 입출력 스트림 초기화
		ServerController.serverPool.submit(new Runnable() {
			@Override
			public void run() {
				try {
					reader = new BufferedReader(new InputStreamReader(gameRoomSocket.getInputStream()));
					writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(gameRoomSocket.getOutputStream())), true);

					// 클라이언트로부터 받은 데이터 처리
					// receive

					while (roomIsRun) {
						
						String receiveData = reader.readLine();
						if (receiveData == null) {
							break;
						}
						String[] data = receiveData.split("\\|");
						String code = data[0];
						String text = data[1];
						// 회원의 id pw 전달
						if (code.equals("0")) {
							String[] info = text.split(",");

							MemberVO member = new MemberVO(info[0], info[1]);
							// gameRoomSocket목록에 등록
							if(masterTask.getRoomUserList().size()<4) {
								
								masterTask.getRoomUserList().put(member, writer);
								roomMember = member;
								// 방입장 성공
								writer.println("l|" + info[0] + "," + info[1]);
								// gameRoomSocket 목록 갱신
								String sendData = "";
								for (MemberVO m : masterTask.getRoomUserList().keySet()) {
									sendData += m.getMId() + "," + m.getPoint();
									sendData += " ";
								}
								masterTask.roomBroadCast("0", sendData);
							}else {
								writer.println("roomEnterFail|full");
								break;
							}
							
							// 문제 시작시 정답 저장
						} else if (code.equals("t")) { 
							ServerController.sc.answer.put(roomName, text);
							// 게임 내 채팅
							// 문자열이 정답과 일치하면 정답 처리
						} else if (code.equals("2")) {
							String[] str = text.split(",");
							masterTask.roomBroadCast("2",  roomMember.getMId() + ":" + str[0]);
							
							if (ServerController.sc.answer.get(roomName) != null && str[0].equals(ServerController.sc.answer.get(roomName))) {
								writer.println("a|true," + roomMember.getMId() + "," + str[1]);
								ServerController.sc.roomWriter.put(roomName, writer);
								masterTask.roomBroadXY("a", "false," + roomMember.getMId() + "," + str[1]);
							}
							
						} else if (code.equals("x")) {
							// x, y 좌표 전송
							masterTask.roomBroadCast("x", text);

							// ColorPicker 색 정보 전송
						} else if (code.equals("c")) {
							masterTask.roomBroadCast("c", text);

							// LineWidth 정보 전송
						} else if (code.equals("w")) {
							masterTask.roomBroadCast("w", text);

							// clear버튼 클릭시 캔버스 클리어
						} else if (code.equals("r")) {
							masterTask.roomBroadCast("r", "clear");

						} else if (code.equals("stop")) {
							roomIsRun = false;
							
						// 방 만들기 과정 중 데이터 전달
						}else if(code.equals("roomName")) {
							ServerController.sc.roomList.put(text, ServerController.roomPort);
		            		String sendData = "";
		            		roomName = text;
		            		for(String s : ServerController.sc.roomList.keySet()) {
		            			sendData += s+" ";
		            		}
		            		masterTask.broadCast("roomList", sendData);
						// 방 입장하기 과정 중 클라이언트에서 방 제목 전달
						}else if(code.equals("roomTitle")) {
							roomName = text;
							masterTask.setRoomName(roomName);
						}
					} // while

					// 클라이언트 연결 종료
					reader.close();
					writer.close();
				} catch (IOException e) {
					roomIsRun = false;
				}
				// while- task 작업 종료
				// gameRoomSocket 소켓도 종료

				if (gameRoomSocket != null && !gameRoomSocket.isClosed()) {
					try {
						gameRoomSocket.close();
					} catch (IOException e) {
					}
				}
				masterTask.getRoomUserList().remove(roomMember);
				
				if (masterTask.getRoomUserList().size() == 0) {
					ServerController.sc.roomList.remove(roomName);
					String sendData = "";
					for (String s : ServerController.sc.roomList.keySet()) {
						sendData += s + " ";
					}

					if (!sendData.equals("")) {
						masterTask.broadCast("roomList", sendData);
						return;
					}
					masterTask.broadCast("roomListClear", "clear");
				}
			}// run
		});
    }
}

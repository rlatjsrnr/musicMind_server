package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
//import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;

import dao.MemberVO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class ServerController implements Initializable{
	
	@FXML private TextArea displayText;
	@FXML private TextField txtPort;
	@FXML private Button btnStartStop;
	
	/**
	  각 게임 방마다 생성되는 소캣의 포트번호
	  방 생성 시 1씩 증가
	 */
	static int roomPort = 6000;
	
	/**
	 *  Client Thread를 관리 할 스레드 풀
	 */
	public static ExecutorService serverPool;
	/**
	 * ServerController
	 */
	public static ServerController sc;
	
	/**
	 * 연결된 client를 관리할 서버소캣
	 */
	ServerSocket server;
	
	/**
	  게임에 입장하는 모든 유저의 객체와 프린터를 저장해둔 Hashtable
	 */
	Hashtable<MemberVO,PrintWriter> clients;
	
	/**
	  만들어진 방의 방이름과 포트번호를 저장한 Hashtable
	 */
	Hashtable<String, Integer> roomList;
	
	/**
	  방 이름과 방별로 진행되는 게임의 문제별 정답 저장
	 */
	Hashtable<String, String> answer;
	
	/**
	  방 이름과 게임 진행 중 정답자의 출력스트림 저장 
	 */
	Hashtable<String, PrintWriter> roomWriter;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		sc = this;
		btnStartStop.setOnAction(e->{
			String text = btnStartStop.getText();
			// _Start
			if(text.equals("_Start")) {
				startServer();
				btnStartStop.setText("S_top");
			}else {
				stopServer();
				btnStartStop.setText("_Start");
			}
		});
	}
	
	// server 시작
	public void startServer() {
		serverPool = Executors.newFixedThreadPool(50);
		clients = new Hashtable<>();
		roomList = new Hashtable<>();
		answer = new Hashtable<>();
		roomWriter = new Hashtable<>();
		int port = 5001;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			displayText.appendText("서버 연결 오류 : " + e.getMessage()+"\n");
			stopServer();
			return;
		}
		Runnable run = new Runnable() {
			@Override
			public void run() {
				printText("[서버 시작]");
				while(true) {
					try {
						printText("client 연결 대기중....");
						Socket client = server.accept();
						String address = client.getRemoteSocketAddress().toString();
						String message = "[연결 수락 : "+address+"]";
						printText(message);
						// 연결된 client마다 전달 메세지를 receive할 수 있도록
						// 스레드 풀에 작업을 정의한 ServerTask 전달
						serverPool.submit(new ServerTask(client));
					} catch (IOException e) {
						stopServer();
						break;
					}
				}
			}
		};
		// 스레드 풀에 수행될 작업 전달
		serverPool.submit(run);
	}
	
	// 작업 스레드 에서 textArea에 출력하는 UI 작업을 처리
	public void printText(String text) {
		Platform.runLater(()->{
			displayText.appendText(text+"\n");
		});
	}
	
	// 자원해제 후 서버 종료
	public void stopServer() {
		
		if(clients != null) {
			for(PrintWriter p : clients.values()) {
				if(p != null) {
					p.close();
				}
			}
		}
		
		clients.clear();
		
		// serverSocket 종료
		try {
			if(server != null && !server.isClosed()) {
				server.close();
			}
			
			if(serverPool != null && !serverPool.isShutdown()) {
				serverPool.shutdownNow();
			}
			
			printText("[서버 중지]");
			
		} catch (IOException e) {}
	}
}
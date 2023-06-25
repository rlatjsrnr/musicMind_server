package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import utils.DBUtil;

public class MemberDAOImpl implements MemberDAO {

	Connection conn;
	Statement stmt;
	PreparedStatement pstmt;
	ResultSet rs;
	
	public MemberDAOImpl() {
		conn = DBUtil.getConnection();
	}
	
	// 로그인시 db정보 확인
	@Override
	public MemberVO selectMember(String mId, String mPw) {
		MemberVO member = null;
		String sql = "SELECT * FROM member WHERE mId = ? AND mPw = ?";
		
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, mId);
			pstmt.setString(2, mPw);
			
			rs = pstmt.executeQuery();
			
			if(rs.next()) {
				member = new MemberVO(
					rs.getString("mId"),		
					rs.getString("mPw")
				);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			DBUtil.close(rs,pstmt);
		}
		return member;
	}
	
	// 회원목록(id,점수)
	@Override
	public ArrayList<MemberVO> select() {
		ArrayList<MemberVO> list = new ArrayList<>();
		
		String sql = "SELECT * FROM member ORDER BY mNum DESC";
		
		try {
			stmt = conn.createStatement();
			rs = stmt.executeQuery(sql);
			while(rs.next()) {
				MemberVO member = new MemberVO();
				String mId = rs.getString(2);
				int point = rs.getInt(4);
				member.setmId(mId);
				member.setPoint(point);
				list.add(member);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(rs,stmt);
		}
		return list;
	}
	
	// 회원 점수 업데이트 
	@Override
	public int update(MemberVO member) {
		int result = 0;
		String sql = "UPDATE member SET mpoint = ? WHERE mId = ?";
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setInt(1, member.getPoint());
			pstmt.setString(2, member.getMId());
			result = pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(pstmt);
		}
		return result;
	}
	
	// 회원가입 시 검증
	public boolean join(String id, String pw) {
		boolean joinResult = false;
		conn = DBUtil.getConnection();
		// 아이디가 기존에 존재하지 않으면 데이터베이스에 삽입 후 success반환 
		String sql = "INSERT INTO member(mId, mPw) VALUES(?,?)";
		try {
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, id);
			pstmt.setString(2, pw);
			int result = pstmt.executeUpdate();
			if(result == 1) {
				joinResult = true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			DBUtil.close(rs, stmt, pstmt);
		}
		return joinResult;
	}
}

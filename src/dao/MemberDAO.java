package dao;

import java.util.ArrayList;

public interface MemberDAO {
		
		// 회원 검색
		// mId , mPw가 일치하는 사용자 검색
		MemberVO selectMember(String mId, String mPw);
		
		// 회원 목록 검색
		ArrayList<MemberVO> select();
		
		// 회원 점수 업데이트 
		int update(MemberVO member);
		
		// 회원 가입 시 검증
		boolean join(String id, String pw);
	
}

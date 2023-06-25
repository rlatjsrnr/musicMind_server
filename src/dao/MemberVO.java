package dao;

// game 패키지에 넣어주세요
public class MemberVO {

	public String mId;
	private String mPw;
	// 점수
	private	int mPoint;
	
	public MemberVO() {}
	
	public MemberVO(String mId, String mPw) {
		this.mId = mId;
		this.mPw = mPw;
	}
	//결과화면 test용 
	public MemberVO(String mId, int point) {
		this.mId = mId;
		this.mPoint = point;
	}

	public String getMId() {
		return mId;
	}

	public void setmId(String mId) {
		this.mId = mId;
	}

	public String getmPw() {
		return mPw;
	}

	public void setmPw(String mPw) {
		this.mPw = mPw;
	}

	public int getPoint() {
		return mPoint;
	}

	public void setPoint(int point) {
		this.mPoint = point;
	}

	@Override
	public String toString() {
		return "MemberVO [mId=" + mId + ", mPw=" + mPw + ", point=" + mPoint + "]";
	}

}

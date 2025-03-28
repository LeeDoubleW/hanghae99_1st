package io.hhplus.tdd.point;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

@Service
public class PointService {
	
	@Autowired
	UserPointTable pointTable;
	
	@Autowired
	PointHistoryTable historyTable;
	
	private ReentrantLock lock = new ReentrantLock();
	
	final long MAXPOINT = 100000000L; 
	
	// 유저조회
	public UserPoint getUser(long id) {
		// id 0일 경우 처리
		if(id < 0) {
			throw new IllegalArgumentException();
		}
		
		return pointTable.selectById(id);
	}
	
	// 유저 포인트 내역 조회
	public List<PointHistory> getPointList(long id) {
		// id 0일 경우 처리
		if(id < 0) {
			throw new IllegalArgumentException();
		}
		
		return historyTable.selectAllByUserId(id);
	}
	
	// 포인트 충전
	public UserPoint chargePoint(long id, long amount) throws Exception {
		lock.lock();
		try {
			// 충전시 금액 확인
			if(amount <= 0) {
				throw new IllegalArgumentException();
			}
			
			// 유저정보 조회후 포인트 계산
			UserPoint user = pointTable.selectById(id);
			long calPoint = user.point() + amount;
			
			if(calPoint > MAXPOINT) {
				throw new Exception("충전가능 한도를 초과하여 충전을 할수없습니다.");
			}
			
			// 포인트 등록후 저장된 값이 요청한 값과 같으면 내역에 추가 진행
			UserPoint insertUserData = pointTable.insertOrUpdate(id, calPoint); 
			if(insertUserData.point() == calPoint) {
				historyTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
			} else {
				throw new Exception("포인트 충전에 실패하였습니다.");
			}
			return insertUserData;
		} finally {
			lock.unlock();
		}
	}
	
	// 포인트 사용
	public UserPoint usePoint(long id, long amount) throws Exception {
		lock.lock();
		try {
			// 충전시 금액 확인
			if(amount <= 0) {
				throw new IllegalArgumentException();
			}
			
			// 유저정보 조회후 포인트 계산
			UserPoint user = pointTable.selectById(id);
			long calPoint = user.point() - amount;
			
			if(calPoint < 0) {
				throw new Exception("보유 포인트가 부족하여 사용할수없습니다.");
			}
			
			// 포인트 등록후 저장된 값이 요청한 값과 같으면 내역에 추가 진행
			UserPoint insertUserData = pointTable.insertOrUpdate(id, calPoint); 
			if(insertUserData.point() == calPoint) {
				historyTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
			} else {
				throw new Exception("포인트 충전에 실패하였습니다.");
			}
			return insertUserData;
		} finally {
			lock.unlock();
		}
	}

}

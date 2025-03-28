package io.hhplus.tdd;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import lombok.extern.slf4j.Slf4j;

/*
 *  통합 테스트 - V포즈를 예로들면 V포즈를 취하는게 통합테스트
 */

@Slf4j
@SpringBootTest
class PointIntegrationTest {
	
	@Autowired
	private PointService ps;
	
	@Autowired
	private PointHistoryTable pht;
	
	private final int THREAD_COUNT = 100;  // 스레드수

	
	@DisplayName("충전 금액이 0이하일 경우 예외가 발생")
    @Test
    void testChargeUserPoint_NegativeOrZeroAmount_ThrowsException() {
        long id = 1L;

        // 0원 충전 시 예외 확인
        assertThatThrownBy(() -> ps.chargePoint(id, 0L))
                .isInstanceOf(IllegalArgumentException.class);

        // 음수 금액 충전 시 예외 확인
        assertThatThrownBy(() -> ps.chargePoint(id, -10000L))
                .isInstanceOf(IllegalArgumentException.class);
    }
	
	@DisplayName("포인트 충전시 한도를 넘으면 예외 발생")
    @Test
    void chargePoint_ExceedsLimit_ThrowsException() throws Exception {
        // given
        long id = 2L;
        long amount = 95000000L;
        long chargeAmount = 10000000L;
        ps.chargePoint(id, amount); // 초기 포인트 설정

        // when & then
        assertThatThrownBy(() -> ps.chargePoint(id, chargeAmount))
                .isInstanceOf(Exception.class).hasMessage("충전가능 한도를 초과하여 충전을 할수없습니다.");
    }
	
	@DisplayName("포인트 충전시 성공")
	@Test
	void chargePoint_Success_Test() throws Exception {
		// given
		long id = 3L;
		long amount = 30000000L;
		TransactionType type = TransactionType.CHARGE;
		long updateMillis = System.currentTimeMillis();
		
		UserPoint user = ps.getUser(id); 
		
		assertThat(user.point()).isEqualTo(0L);
		
		UserPoint charge = ps.chargePoint(id, amount);
		
		assertThat(charge.point()).isEqualTo(amount);
		
		PointHistory ps = pht.insert(id, amount, type, updateMillis);
		
		assertThat(ps.userId()).isEqualTo(id);
		assertThat(ps.amount()).isEqualTo(amount);
		assertThat(ps.type()).isEqualTo(type);
		assertThat(ps.updateMillis()).isEqualTo(updateMillis);
	}
	
	@DisplayName("포인트 사용시 보유 포인트가 사용 포인트보다 적을때 예외 발생")
	@Test
    void usePoint_InsufficientBalance_ThrowsException() throws Exception {
        // given
        long id = 4L;
        long amount = 2000L;
        long useAmount = 3000L;
        UserPoint user = ps.chargePoint(id, amount); // 초기 포인트 설정

        // when & then
        assertThatThrownBy(() -> ps.usePoint(id, useAmount))
                .isInstanceOf(Exception.class).hasMessage("보유 포인트가 부족하여 사용할수없습니다.");
    }
	
	@DisplayName("포인트 사용시 성공")
	@Test
	void usePoint_Success_Test() throws Exception {
		// given
		long id = 5L;
		long amount = 30000000L;
		long useAmount = 10000000L;
		TransactionType type = TransactionType.USE;
		long updateMillis = System.currentTimeMillis();
		
		UserPoint user = ps.chargePoint(id, amount); 
		
		UserPoint use = ps.usePoint(id, useAmount);
		
		assertThat(use.point()).isEqualTo(user.point() - useAmount);
		
		PointHistory ps = pht.insert(id, amount, type, updateMillis);
		
		assertThat(ps.userId()).isEqualTo(id);
		assertThat(ps.amount()).isEqualTo(amount);
		assertThat(ps.type()).isEqualTo(type);
		assertThat(ps.updateMillis()).isEqualTo(updateMillis);
	}

    @DisplayName("유저의 결제 내역을 조회한다.")
    @Test
    void getUserPointHistory() throws Exception {
        // given
        long id = 6L;
        long chargeAmount = 10000L;
        long useAmount = 3000L;

        // when
        // 포인트 충전
        ps.chargePoint(id, chargeAmount);

        // 포인트 차감
        ps.usePoint(id, useAmount);

        // then
        // 결제 내역 확인
        List<PointHistory> pointHistoryList = ps.getPointList(id);
        assertThat(pointHistoryList).hasSize(2);
        assertThat(pointHistoryList.get(0).amount()).isEqualTo(chargeAmount);
        assertThat(pointHistoryList.get(1).amount()).isEqualTo(useAmount);
    }
    
    @Test
    @DisplayName("동일한 ID에 100개의 충전 요청이 동시에 들어온다.")
    public void concurrentChargeUserPoint() throws Exception {
        long id = 7L;
        long firstAmount = 1000L;
        long threadAmount = 500L;
        // given
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        ps.chargePoint(id, firstAmount);

        // when
        // 스레드 생성 및 실행
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    ps.chargePoint(id, threadAmount);
                } catch (Exception e) {
					e.printStackTrace();
				} finally {
                    latch.countDown(); // 작업 완료 시 카운트 감소
                }
            });
        }
        
        latch.await(); // 스레드 작업이 모두 완료될 때까지 대기
        executor.shutdown(); // 스레드 풀 종료

        // then
        long expectedTotal = firstAmount + (threadAmount * THREAD_COUNT);
        log.info("charge total : " + expectedTotal);
        UserPoint userPoint = ps.getUser(id);
        assertThat(userPoint.point()).isEqualTo(expectedTotal);
    }


    @Test
    @DisplayName("동일한 ID에 100개의 사용 요청이 동시에 들어온다.")
    public void concurrentDeductPoint() throws Exception {
        long id = 8L;
        long firstAmount = 100000L;
        long threadAmount = 500L;
        // given
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        ps.chargePoint(id, firstAmount);

        // when
        // 스레드 생성 및 실행
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    ps.usePoint(id, threadAmount);
                } catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
                    latch.countDown(); // 작업 완료 시 카운트 감소
                }
            });
        }

        latch.await(); // 스레드 작업이 모두 완료될 때까지 대기
        executor.shutdown(); // 스레드 풀 종료

        // then
        // 최종 포인트 확인
        long expectedTotal = (firstAmount - (threadAmount * THREAD_COUNT) <= 0) ? 0 : (firstAmount - (threadAmount * THREAD_COUNT));
        log.info("use total : " + expectedTotal);
        UserPoint userPoint = ps.getUser(id);
        assertThat(userPoint.point()).isEqualTo(expectedTotal); // 최종 포인트는 차감된 금액
    }
}

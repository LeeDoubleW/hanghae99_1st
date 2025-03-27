package io.hhplus.tdd;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;

/*
 *  단위 테스트 - V포즈를 예로들면 엄지와 검지가 단위테스트
 */

class PointUnitTest {

	@Mock
	private UserPointTable userRepository;
	
	@Mock
	private PointHistoryTable historyRepository;
	
	@InjectMocks
	private PointService ps;

	@BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
	
	@DisplayName("음수가 아닌 ID로 유저를 조회")
	@Test
	void getUserFuncTest() {
		// given
		long id = 1L;
		UserPoint user = UserPoint.empty(id);
		when(userRepository.selectById(id)).thenReturn(user);
		
		// when		
		// then
		assertThat(ps.getUser(id)).isEqualTo(user);
	}
	
	@DisplayName("음수인 ID로 유저를 조회")
	@Test
	void getUser_Negative_Test() {
		// given
		long id = -1L;
		when(userRepository.selectById(id)).thenThrow(IllegalArgumentException.class);
		
		// when		
		// then
		assertThatThrownBy(() -> ps.getUser(id)).isInstanceOf(IllegalArgumentException.class);
	}
	
	@DisplayName("충전 포인트가 0 미만일 경우 예외 발생")
	@Test
	void charge_Negative_Test() {
		// given
		long id = 1L;
		long amount = -100L;
		when(userRepository.insertOrUpdate(id, amount)).thenThrow(IllegalArgumentException.class);
		
		// when		
		// then
		assertThatThrownBy(() -> ps.chargePoint(id, amount)).isInstanceOf(IllegalArgumentException.class);
	}
	
	@DisplayName("충전 포인트가 0 일 경우 예외 발생")
	@Test
	void charge_Zero_Test() {
		// given
		long id = 1L;
		long amount = 0L;
		when(userRepository.insertOrUpdate(id, amount)).thenThrow(IllegalArgumentException.class);
		
		// when		
		// then
		assertThatThrownBy(() -> ps.chargePoint(id, amount)).isInstanceOf(IllegalArgumentException.class);
	}
	
	@DisplayName("포인트 충전시 한도를 넘으면 예외 발생")
	@Test
	void charge_Max_Test() {
		// given
		long id = 1L;
		long amount = 30000000L;
		
		when(userRepository.selectById(id)).thenReturn(new UserPoint(id, 80000000L, System.currentTimeMillis()));
		
		// when		
		// then
		assertThatThrownBy(() -> ps.chargePoint(id, amount))
			.isInstanceOf(Exception.class).hasMessage("충전가능 한도를 초과하여 충전을 할수없습니다.");
	}
	
	@DisplayName("사용 포인트가 0 미만일 경우 예외 발생")
	@Test
	void use_Negative_Test() {
		// given
		long id = 1L;
		long amount = -100L;
		when(userRepository.insertOrUpdate(id, amount)).thenThrow(IllegalArgumentException.class);
		
		// when		
		// then
		assertThatThrownBy(() -> ps.chargePoint(id, amount)).isInstanceOf(IllegalArgumentException.class);
	}
	
	@DisplayName("사용 포인트가 0 일 경우 예외 발생")
	@Test
	void use_Zero_Test() {
		// given
		long id = 1L;
		long amount = 0L;
		when(userRepository.insertOrUpdate(id, amount)).thenThrow(IllegalArgumentException.class);
		
		// when		
		// then
		assertThatThrownBy(() -> ps.usePoint(id, amount)).isInstanceOf(IllegalArgumentException.class);
	}
	
	@DisplayName("포인트 사용시 보유 포인트가 사용 포인트보다 적을때 예외 발생")
	@Test
	void use_Low_Test() {
		// given
		long id = 1L;
		long amount = 20000000L;
		
		when(userRepository.selectById(id)).thenReturn(new UserPoint(id, 10000000L, System.currentTimeMillis()));
		
		// when		
		// then
		assertThatThrownBy(() -> ps.usePoint(id, amount))
			.isInstanceOf(Exception.class).hasMessage("보유 포인트가 부족하여 사용할수없습니다.");
	}
}

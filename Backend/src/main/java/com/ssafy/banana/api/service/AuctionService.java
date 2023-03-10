package com.ssafy.banana.api.service;

import static java.time.LocalDateTime.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.ssafy.banana.db.entity.Art;
import com.ssafy.banana.db.entity.Auction;
import com.ssafy.banana.db.entity.AuctionBidLog;
import com.ssafy.banana.db.entity.AuctionJoin;
import com.ssafy.banana.db.entity.AuctionJoinId;
import com.ssafy.banana.db.entity.Curation;
import com.ssafy.banana.db.entity.CurationArt;
import com.ssafy.banana.db.entity.User;
import com.ssafy.banana.db.entity.enums.AuctionStatus;
import com.ssafy.banana.db.repository.ArtRepository;
import com.ssafy.banana.db.repository.AuctionBidLogRepository;
import com.ssafy.banana.db.repository.AuctionJoinRepository;
import com.ssafy.banana.db.repository.AuctionRepository;
import com.ssafy.banana.db.repository.CurationArtRepository;
import com.ssafy.banana.db.repository.CurationRepository;
import com.ssafy.banana.db.repository.UserRepository;
import com.ssafy.banana.dto.request.AuctionRequest;
import com.ssafy.banana.dto.request.CountdownRequest;
import com.ssafy.banana.dto.response.AuctionResponse;
import com.ssafy.banana.dto.response.AuctionUpdateResponse;
import com.ssafy.banana.exception.CustomException;
import com.ssafy.banana.exception.CustomExceptionType;
import com.ssafy.banana.util.SseEmitterUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionService {
	private final UserRepository userRepository;
	private final CurationArtRepository curationArtRepository;
	private final AuctionRepository auctionRepository;
	private final AuctionJoinRepository auctionJoinRepository;
	private final ArtRepository artRepository;
	private final CurationRepository curationRepository;
	private final AuctionBidLogRepository auctionBidLogRepository;
	private final SseEmitterUtil sseEmitterUtil;

	/**
	 * ?????? ??????
	 * @param curationArtSeq ?????? ?????? pk
	 * @param userSeq ?????? ?????? ?????? ?????? pk
	 * @return
	 */
	@Transactional
	public void joinAuction(long curationArtSeq, long userSeq) {

		CurationArt curationArt = curationArtRepository.findById(curationArtSeq)
			.orElseThrow(() -> new CustomException(CustomExceptionType.RUNTIME_EXCEPTION));

		// ?????? ?????? ??????, ?????? ????????? ????????? ????????? ?????? ????????? ?????? ?????? ??????
		if (userSeq == curationArt.getCuration().getArtist().getId()
			|| curationArt.getAuctionStartPrice() == 0) {
			throw new CustomException(CustomExceptionType.AUCTION_FAIL);
		}
		User user = userRepository.findById(userSeq)
			.orElseThrow(() -> new CustomException(CustomExceptionType.RUNTIME_EXCEPTION));

		// ?????? ?????? ?????? ??????
		AuctionJoinId auctionJoinId = AuctionJoinId.builder()
			.userSeq(user.getId())
			.curationArtSeq(curationArt.getId())
			.build();
		// ?????? ????????? ??????
		if (auctionJoinRepository.findById(auctionJoinId).isPresent()) {
			throw new CustomException(CustomExceptionType.AUCTION_JOIN_CONFLICT);
		}
		AuctionJoin auctionJoin = AuctionJoin.builder()
			.id(auctionJoinId)
			.user(user)
			.curationArt(curationArt)
			.auctionJoinTime(now())
			.build();
		auctionJoinRepository.save(auctionJoin);

		// ???????????? ?????? ?????? - ?????? ????????? ??? ??????
		int auctionPeopleCount = auctionJoinRepository.countAuctionJoinPeople(curationArtSeq);
		curationArt.setAuctionPeopleCnt(auctionPeopleCount);
		curationArtRepository.save(curationArt);
	}

	/**
	 * ?????? ?????? (?????? ?????????, ?????? ?????? ?????? ????????? ?????????)
	 * @param curationSeq ???????????? pk
	 * @param userSeq ????????? ?????? pk
	 */
	@Transactional
	public void createAuction(long curationSeq, long userSeq) {

		Curation curation = curationRepository.findById(curationSeq)
			.orElseThrow(() -> new CustomException(CustomExceptionType.RUNTIME_EXCEPTION));

		// ?????? ????????? ????????? ?????? ?????? ??????
		User artist = curation.getArtist().getUser();
		long artistSeq = artist.getId();
		if (artistSeq != userSeq) {
			throw new CustomException(CustomExceptionType.AUTHORITY_ERROR);
		}
		// ?????? ????????? ?????? ????????? ( ?????? ?????? ?????? 0??? ?????????, ?????? ?????? ????????? ?????? )
		List<CurationArt> curationArtList =
			curationArtRepository.findByCuration_IdAndAuctionStartPriceNotAndAuctionPeopleCntNotOrderById(
					curation.getId(), 0,
					0)
				.orElseThrow(() -> new CustomException(CustomExceptionType.UNABLE_AUCTION));

		for (CurationArt curationArt : curationArtList) {
			// ?????? ?????????
			// ?????? ?????? ????????? ?????????
			if (auctionRepository.findById(curationArt.getId()).isPresent()) {
				throw new CustomException(CustomExceptionType.AUCTION_INFO_CONFLICT);
			}
			LocalDateTime currentTime = LocalDateTime.now();
			// ?????? ?????? ?????????
			Auction auction = Auction.builder()
				.id(curationArt.getId())
				.curationArt(curationArt)
				.auctionStartPrice(curationArt.getAuctionStartPrice())
				.auctionGap(curationArt.getAuctionGap())
				.auctionStartTime(currentTime)
				.auctionEndTime(currentTime)
				.auctionPaidTime(currentTime)
				.auctionStatusTime(currentTime)
				.auctionEndPrice(curationArt.getAuctionStartPrice())
				.auctionStatus(AuctionStatus.INIT)
				.user(artist)
				.build();
			auctionRepository.save(auction);

			// ?????? ?????? ????????? (?????? ???????????? ????????? ??????)
			AuctionBidLog auctionBidLog = AuctionBidLog.builder()
				.auctionBidPrice(auction.getAuctionStartPrice())
				.auctionBidTime(currentTime)
				.user(artist)
				.auction(auction)
				.build();
			auctionBidLogRepository.save(auctionBidLog);
		}
	}

	/**
	 * ?????? ?????? (?????? ?????? ??????)
	 * @param curationSeq ???????????? pk
	 * @return ?????? ?????? ?????? DTO
	 */
	@Transactional
	public AuctionResponse getAuctionInfo(long curationSeq) {

		Curation curation = curationRepository.findById(curationSeq)
			.orElseThrow(() -> new CustomException(CustomExceptionType.RUNTIME_EXCEPTION));

		// ?????? ????????? ?????? ???, ????????? ???????????? ?????? ??????
		Auction auction = auctionRepository.findAuctionInfo(curationSeq, AuctionStatus.INIT)
			.orElseThrow(() -> new CustomException(CustomExceptionType.UNABLE_AUCTION));

		CurationArt curationArt = auction.getCurationArt();
		// ?????? ????????? ?????? ?????? ??????
		Art art = artRepository.findById(auction.getCurationArt().getArt().getId())
			.orElseThrow(() -> new CustomException(CustomExceptionType.NO_CONTENT));

		LocalDateTime currentTime = LocalDateTime.now();
		User artist = curation.getArtist().getUser();
		AuctionResponse auctionResponse = AuctionResponse.builder()
			.artistSeq(artist.getId())
			.artistNickname(artist.getNickname())
			.artImg(art.getArtImg())
			.artName(art.getArtName())
			.artDescription(art.getArtDescription())
			.auctionStartPrice(auction.getAuctionStartPrice())
			.auctionCurrentPrice(auction.getAuctionStartPrice())
			.auctionBidPrice(auction.getAuctionStartPrice() + curationArt.getAuctionGap())
			.auctionEndTime(currentTime.plusMinutes(1))
			.message("[HOST] ????????? ?????????????????????.")
			.build();

		auction
			.setAuctionStatus(AuctionStatus.ONGOING)
			.setAuctionStatusTime(currentTime)
			.setAuctionStartTime(currentTime)
			.setAuctionEndTime(currentTime.plusMinutes(1));
		auctionRepository.save(auction);

		return auctionResponse;
	}

	/**
	 * ?????? ?????? ?????? ????????????
	 * @param auctionRequest ?????? ?????? ?????? ?????? DTO
	 * @param userSeq ????????? ?????? pk
	 * @return ?????? ?????? ?????? ?????? DTO
	 */
	@Transactional
	public AuctionUpdateResponse updateAuction(AuctionRequest auctionRequest, long userSeq) {

		Auction auction = auctionRepository.findById(auctionRequest.getCurationArtSeq())
			.orElseThrow(() -> new CustomException(CustomExceptionType.RUNTIME_EXCEPTION));

		// ?????? ?????? ????????? ??????
		if (auction.getAuctionStatus() != AuctionStatus.ONGOING) {
			throw new CustomException(CustomExceptionType.AUCTION_NOT_ONGOING);
		}
		// ????????? ????????? ?????? ?????? ??????
		long artistSeq = auction.getCurationArt().getCuration().getArtist().getId();
		if (userSeq == artistSeq) {
			throw new CustomException(CustomExceptionType.AUCTION_FAIL);
		}
		// ?????????
		User user = userRepository.findById(userSeq)
			.orElseThrow(() -> new CustomException(CustomExceptionType.RUNTIME_EXCEPTION));

		// ?????? ??????
		AuctionBidLog auctionBidLog = auctionBidLogRepository.findTopByAuction_IdOrderByIdDesc(
			auctionRequest.getCurationArtSeq());

		LocalDateTime currentTime = LocalDateTime.now();
		// ?????? ?????? ??????
		auctionBidLog = AuctionBidLog.builder()
			.auctionBidPrice(auctionBidLog.getAuctionBidPrice() + auction.getAuctionGap())
			.auctionBidTime(currentTime)
			.user(user)
			.auction(auction)
			.build();
		auctionBidLogRepository.save(auctionBidLog);

		auction.setAuctionEndTime(currentTime.plusMinutes(1));
		auctionRepository.save(auction);
		sseEmitterUtil.bidding(auctionRequest.getCurationArtSeq(), user.getNickname(),
			auctionBidLog.getAuctionBidPrice());

		int currentPrice = auctionBidLog.getAuctionBidPrice();
		AuctionUpdateResponse auctionUpdateResponse = AuctionUpdateResponse.builder()
			.auctionCurrentPrice(currentPrice)
			.auctionBidPrice(currentPrice + auction.getAuctionGap())
			.auctionEndTime(auction.getAuctionEndTime())
			.message(String.format("[ %s ] ?????? ????????? %d???", user.getNickname(), currentPrice))
			.build();

		return auctionUpdateResponse;
	}

	/**
	 * ?????? ?????? ??????
	 * @param curationArtSeq ???????????? ?????? pk
	 */
	@Transactional
	public void closeOneAuction(long curationArtSeq) {

		Auction auction = auctionRepository.findById(curationArtSeq)
			.orElseThrow(() -> new CustomException(CustomExceptionType.RUNTIME_EXCEPTION));

		if (auction.getAuctionStatus() == AuctionStatus.ONGOING) {
			// ?????? ???????????? ?????? ??????(??????)?????? FAILED, ?????????????????? SUCCESS
			long artistSeq = auction.getCurationArt().getCuration().getArtist().getId();
			AuctionBidLog auctionBidLog = auctionBidLogRepository.findTopByAuction_IdOrderByIdDesc(curationArtSeq);
			if (artistSeq == auctionBidLog.getUser().getId()) {
				auction.setAuctionStatus(AuctionStatus.FAILED);
			} else {
				auction
					.setAuctionStatus(AuctionStatus.SUCCESS)
					.setAuctionEndPrice(auctionBidLog.getAuctionBidPrice())
					.setUser(auctionBidLog.getUser());
			}

			LocalDateTime currentTime = LocalDateTime.now();
			auction
				.setAuctionStatusTime(currentTime)
				.setAuctionEndTime(currentTime);
			auctionRepository.save(auction);
			sseEmitterUtil.closeAllSession(curationArtSeq);
		} else if (auction.getAuctionStatus() == AuctionStatus.INIT) {
			throw new CustomException(CustomExceptionType.NOT_GOING_AUCTION);
		} else {
			throw new CustomException(CustomExceptionType.AUCTION_CLOSE_CONFLICT);
		}
	}

	/**
	 * ?????? ?????? ??????
	 * @param curationSeq ???????????? pk
	 * @param userSeq ????????? ?????? pk
	 */
	@Transactional
	public void closeAllAuction(long curationSeq, long userSeq) {

		Curation curation = curationRepository.findById(curationSeq)
			.orElseThrow(() -> new CustomException(CustomExceptionType.NO_CONTENT));
		// ?????? ????????? ????????? ?????? ?????? ??????
		if (curation.getArtist().getId() != userSeq) {
			throw new CustomException(CustomExceptionType.AUTHORITY_ERROR);
		}
		// ?????? ????????? ?????? ????????? ( ?????? ?????? ?????? 0??? ?????????, ?????? ?????? ????????? ?????? )
		List<CurationArt> curationArtList = curationArtRepository
			.findByCuration_IdAndAuctionStartPriceNotAndAuctionPeopleCntNotOrderById(curation.getId(), 0, 0)
			.orElseThrow(() -> new CustomException(CustomExceptionType.NO_CONTENT));

		int closeAuctionCount = 0;
		for (CurationArt curationArt : curationArtList) {
			Auction auction = auctionRepository.findById(curationArt.getId()).orElse(null);
			if (auction.getAuctionStatus() == AuctionStatus.INIT
				|| auction.getAuctionStatus() == AuctionStatus.ONGOING) {
				// ?????? ???????????? ?????? ??????(??????)?????? FAILED
				Long artistSeq = auction.getCurationArt().getCuration().getArtist().getId();
				AuctionBidLog auctionBidLog = auctionBidLogRepository.findTopByAuction_IdOrderByIdDesc(
					curationArt.getId());
				if (artistSeq == auctionBidLog.getUser().getId()) {
					auction.setAuctionStatus(AuctionStatus.FAILED);
				} else {
					auction
						.setAuctionStatus(AuctionStatus.SUCCESS)
						.setAuctionEndPrice(auctionBidLog.getAuctionBidPrice())
						.setUser(auctionBidLog.getUser());
				}
				LocalDateTime currentTime = LocalDateTime.now();
				auction
					.setAuctionStatusTime(currentTime)
					.setAuctionEndTime(currentTime);
				auctionRepository.save(auction);
				closeAuctionCount++;
				if (auction.getAuctionStatus() == AuctionStatus.ONGOING) {
					sseEmitterUtil.closeAllSession(curationArt.getId());
				}
			}
		}
		// ?????? ????????? ?????? ?????????
		if (closeAuctionCount == 0) {
			throw new CustomException(CustomExceptionType.AUCTION_CLOSE_CONFLICT);
		}
	}

	public SseEmitter connectAuctionHost(long curationArtSeq) {
		SseEmitter emitter = new SseEmitter(0L);
		sseEmitterUtil.connectSession(curationArtSeq, emitter);
		try {
			emitter.send(SseEmitter.event()
				.name("auctionHost")
				.data("?????? ????????? ?????????"));
		} catch (IOException e) {
			throw new CustomException(CustomExceptionType.RUNTIME_EXCEPTION);
		}

		return emitter;
	}

	public void closeAllAuctionHost(long curationArtSeq) {
		sseEmitterUtil.closeAllSession(curationArtSeq);
	}

	public void countdownAuctionHost(CountdownRequest countdownRequest) {
		sseEmitterUtil.countdown(countdownRequest);
	}
}

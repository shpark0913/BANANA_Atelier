import React, { useState } from 'react'
import { Form, Link, redirect } from "react-router-dom";
import { useLoaderData } from 'react-router-dom';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faBookmark } from "@fortawesome/free-solid-svg-icons";
import { axiosAuth, axiosReissue } from '../../_actions/axiosAuth';
import { useNavigate } from 'react-router-dom';
import ProfileImg from "../../components/commons/ProfileImg";
// import CurationComponent from '../../components/commons/CurationComponent';
import ArtItemMyPage from "../../components/MyPage/ArtItemMyPage";
// import { getArtThumbnail } from '../../components/commons/imageModule';
import { getArtImage } from '../../components/commons/imageModule';

import { BookmarkBtn, RedBtn, YellowBtn } from "../../components/commons/buttons";
import '../ArtsPage/ArtsDetail.css'
import '../MyPage/ArtsRoot.css'


export async function loader ({params}) {
  let curationSeq = +params.curation_seq
  axiosReissue();

  let curationDetail, curationDetailArts, isBookmarked = null;
  await axiosAuth.get(`/curations/details/${curationSeq}`)
    .then(response => {
      curationDetail = response.data;
      return curationDetail
    })
    .then(curationDetail => {
      return axiosAuth.get(`/curation-art/list/${curationDetail.curationSeq}`);
    })
    .then(response => {
      curationDetailArts = response.data;
    })
    .catch(error => console.log(error));

  await axiosAuth.get(`/curations/${localStorage.getItem("userSeq")}/${curationDetail.curationSeq}`)
    .then(response => {
      isBookmarked = response.data
    })

  console.log(curationDetail)
  console.log(curationDetailArts)
  console.log(isBookmarked)
  return [curationDetail, curationDetailArts, isBookmarked];
}

export async function action ({request, params}) {
  const curationSeq = +params.curation_seq;
  if (request.method === "DELETE") {
    await axiosAuth.delete(`curations/${curationSeq}`)
      .then(response => console.log(response))
      .catch(error => console.log(error))
  }
  else if (request.method === "PUT") {
    console.log("put")
  }
  return redirect('../')
}

function CurationsDetail() {
  const [curationDetail, curationDetailArts, isBookmarked] = useLoaderData();
  const navigate = useNavigate()

  let nickname= curationDetail.userNickname
  let profileImg= curationDetail.profileImg
  let userSeq = curationDetail.userSeq
  // let curationThumbnail =curationDetail.curationThumbnail
  let curationImg = curationDetail.curationImg
  let curationName =curationDetail.curationName
  let curationSeq = curationDetail.curationSeq
  // let curationHit = curationDetail.curationHit
  let curationBmCount = curationDetail.curationBmCount
  let curationStartTime = curationDetail.curationStartTime
  let curationStatus = curationDetail.curationStatus
  let curationSummary = curationDetail.curationSummary

  const [bookmarkNum, setBookmarkNum] = useState(curationBmCount)
  const [likeCurations, setLikeCurations] = useState(isBookmarked)
  const handleBookMark = async () => {
    console.log('??????????????????????')
    let body = {
      curationSeq: curationSeq,
      userSeq: localStorage.getItem("userSeq")
    }
    axiosReissue();

    if (likeCurations) {
      await axiosAuth.delete('curations/bookmark', {data: body})
      setBookmarkNum(prev=>prev-1)
    } else {
      await axiosAuth.post('curations/bookmark', body)
        .then(response => console.log(response))
      setBookmarkNum(prev=>prev+1)
    }
    setLikeCurations(prev=>!prev)
  }

  // ???????????? ?????? (?????????, ??????, ????????? ?????? ????????? ?????????)
  let curationDate;
  if (curationStatus === "INIT") {
      curationDate = <div>{`${curationStartTime[0]}.${(curationStartTime[1]+'').padStart(2, "0")}.${(curationStartTime[2]+'').padStart(2, "0")} ${(curationStartTime[3]+'').padStart(2, "0")}:${(curationStartTime[4]+"").padStart(2, "0")} ??????`}</div>
  } else if (curationStatus === "ON") {
      curationDate = <div>{`${curationStartTime[0]}.${(curationStartTime[1]+'').padStart(2, "0")}.${(curationStartTime[2]+'').padStart(2, "0")} ?????????`}</div>
  } else {
      curationDate = <div>{`${curationStartTime[0]}.${(curationStartTime[1]+'').padStart(2, "0")}.${(curationStartTime[2]+'').padStart(2, "0")} ??????`}</div>
  }


  const handleStartCuration = () => {
    let userSeq = localStorage.getItem("userSeq")
    localStorage.setItem("artistSeq", userSeq)
    axiosReissue()
  
    axiosAuth.put(`curations/${curationSeq}/on`)

    navigate(`/curations/on_air/${curationSeq}`)
  }

  const handleEnterCuration = () => {
    navigate(`/curation/on_air/${curationSeq}`)
  }



  return (
    <div>
      <div className="art-detail__container grid__detail-page">

        {/* ????????? curation detail?????? ????????? ?????? arts???????????? ????????? */}
        {/*<img src={`${getArtThumbnail(curationThumbnail, userSeq)}`} alt="???????????? ?????? ?????????" className="art-img" />*/}
        <img src={`${getArtImage(curationImg, userSeq)}`} alt="???????????? ?????? ?????????" className="art-img" />
        {/* --------------------------------------------------- */}

        {/* ???????????? ?????? ?????? */}
        <div className="art-detail_content">
          <div className="art-detail__main-info">
            <div className="art-detail__title">
              <h1>{curationName}</h1>
              { userSeq === +localStorage.getItem('userSeq') &&
                <div className="art-detail__manage">
                  <Form method="delete"><RedBtn type="submit">????????????</RedBtn></Form>
                  <Form method="put"><YellowBtn type="submit">????????????</YellowBtn></Form>
                </div> }
            </div>
            <Link className="art-detail__profile link" to={`/${nickname}@${userSeq}`}>
              <ProfileImg height="30px" width="30px" url={profileImg} userSeq={userSeq} />
              <div>{nickname} <span className="jakka">??????</span></div>
            </Link>

            <div className="upload_date">{curationDate}</div>
            <div className="arts_description" style={{whiteSpace: "pre-line"}}>
              {curationSummary}
            </div>
          </div>

          <div>

            <div className="art-detail__sub-info">
              <div className="views">
                <FontAwesomeIcon icon={faBookmark} /> {bookmarkNum}
              </div>
            </div>

            <div className="art-detail__btns">
              <div onClick={handleBookMark}>
                <BookmarkBtn  isBookmark={likeCurations} />
              </div>
              {/* ????????? ???????????? ?????? ??????, ??? ????????? ????????? ?????? ????????????~ */}
              { curationStatus === "ON" &&
                <Link to={`../curations/on_air/${curationSeq}`}><YellowBtn style={{width: "120px"}} onClick={handleEnterCuration}>????????????</YellowBtn></Link> }
              {/* ?????? ??? ?????????????????? ????????? ????????? ???????????? ????????? */}
              { (curationStatus === "INIT" && userSeq === +localStorage.getItem('userSeq')) &&
                <Link to={`../curations/on_air/${curationSeq}`} ><YellowBtn style={{width: "120px"}} onClick={handleStartCuration}>????????????</YellowBtn></Link> }
            </div>

          </div>
        </div>
      </div>


      <div className="arts_curation_for art-root__masterpiece-container">
        <h3 style={{gridColumn: '1 / end'}}>???????????? ?????? ??????</h3>
        { curationDetailArts.map((art) =>
          <div key={`curation-detail__art-${art.artSeq}`}>
            <ArtItemMyPage
              artThumbnail={art.curationThumbnail}
              userSeq={userSeq}
              artSeq={art.artSeq}
              artName={art.artName}
              nickname={art.artistNickName}
            />
            <div>?????? ????????? : {art.auctionStartPrice}</div>
            <div>?????? ?????? : {art.auctionGap}</div>
          </div>
        )}
      </div>

    </div>
  )
}

export default CurationsDetail

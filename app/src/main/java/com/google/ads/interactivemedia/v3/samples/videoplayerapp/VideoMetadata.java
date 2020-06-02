package com.google.ads.interactivemedia.v3.samples.videoplayerapp;

import java.util.Arrays;
import java.util.List;

/** An enumeration of video metadata. */
public enum VideoMetadata {
  PRE_ROLL_NO_SKIP(
      "https://storage.googleapis.com/gvabox/media/samples/stock.mp4",
          //"https://vod.mitele.es/hls/VOD/c1b4cd98-4e63-4754-96bb-fd087e40fb43/c1b4cd98-4e63-4754-96bb-fd087e40fb43.,250,450,810,1460,.mp4.csmil/master.m3u8?hdnts=st=1591121606~exp=1591136005~acl=/hls/VOD/c1b4cd98-4e63-4754-96bb-fd087e40fb43/c1b4cd98-4e63-4754-96bb-fd087e40fb43.,250,450,810,1460,.mp4.csmil/*~hmac=1a4d189594a505b2577bbad560700b4daa97b9fdd553adaef4c1b05cea2d0688",
      "Pre-roll, linear not skippable",
//      "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/"
//          + "single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast"
//          + "&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct"
//          + "%3Dlinear&correlator=",
          "https://pubads.g.doubleclick.net/gampad/ads?env=vp&gdfp_req=1&unviewed_position_start=1&description_url=https%3A%2F%2Fwww.mitele.es%2Fprogramas-tv%2Fsabado-deluxe%2F2020%2Fsabado-deluxe-programa-587-40_1008422575001%2Fplayer%2F&output=vmap&iu=/20370287687/appmitele/vid-pub/programas/sabado-deluxe&sz=640x480&ad_rule=1&impl=s&vid=MDSVOD20200517_0004&cmsid=2457003&cust_params=cf%3Dlong_form%26embed%3D1%26videogaleria%3D0%26perule%3D%7Bperule%7D",
      R.drawable.thumbnail1,
      false),
  PRE_ROLL_SKIP(
      "https://storage.googleapis.com/gvabox/media/samples/stock.mp4",
      "Pre-roll, linear, skippable",
      "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/"
          + "single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast"
          + "&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct"
          + "%3Dskippablelinear&correlator=",
      R.drawable.thumbnail1,
      false),
  POST_ROLL(
      "https://storage.googleapis.com/gvabox/media/samples/stock.mp4",
      "Post-roll",
      "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/"
          + "ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp"
          + "&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite"
          + "%26sample_ar%3Dpostonly&cmsid=496&vid=short_onecue&correlator=",
      R.drawable.thumbnail1,
      true),
  VMAP(
      "https://storage.googleapis.com/gvabox/media/samples/stock.mp4",
      "VMAP",
      "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/"
          + "ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp"
          + "&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite"
          + "%26sample_ar%3Dpremidpost&cmsid=496&vid=short_onecue&correlator=",
      R.drawable.thumbnail1,
      true),
  VMAP_PODS(
      "https://storage.googleapis.com/gvabox/media/samples/stock.mp4",
      "VMAP Pods",
      "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/"
          + "ad_rule_samples&ciu_szs=300x250&ad_rule=1&impl=s&gdfp_req=1&env=vp"
          + "&output=vmap&unviewed_position_start=1&cust_params=deployment%3Ddevsite"
          + "%26sample_ar%3Dpremidpostpod&cmsid=496&vid=short_onecue&correlator=",
      R.drawable.thumbnail1,
      true),
  WRAPPER(
      "https://storage.googleapis.com/gvabox/media/samples/stock.mp4",
      "Wrapper",
      "https://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=/124319096/external/"
          + "single_ad_samples&ciu_szs=300x250&impl=s&gdfp_req=1&env=vp&output=vast"
          + "&unviewed_position_start=1&cust_params=deployment%3Ddevsite%26sample_ct"
          + "%3Dredirectlinear&correlator=",
      R.drawable.thumbnail1,
      false),
  CUSTOM(
      "https://storage.googleapis.com/gvabox/media/samples/stock.mp4",
      "Custom",
      "custom",
      R.drawable.thumbnail1,
      false);

  public static final List<VideoMetadata> APP_VIDEOS =
      Arrays.asList(PRE_ROLL_NO_SKIP, PRE_ROLL_SKIP, POST_ROLL, VMAP, VMAP_PODS, WRAPPER, CUSTOM);

  /** The thumbnail image for the video. */
  public final int thumbnail;

  /** The title of the video. */
  public final String title;

  /** The URL for the video. */
  public final String videoUrl;

  /** The ad tag for the video */
  public final String adTagUrl;

  /** If the ad is VMAP. */
  public final boolean isVmap;

  private VideoMetadata(
      String videoUrl, String title, String adTagUrl, int thumbnail, boolean isVmap) {
    this.videoUrl = videoUrl;
    this.title = title;
    this.adTagUrl = adTagUrl;
    this.thumbnail = thumbnail;
    this.isVmap = isVmap;
  }
}

const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");

admin.initializeApp();

const CHANNEL_ID = "UCnHGYAMb_Xpu9QkdVoxzxig"; // https://www.youtube.com/@nivgilboa [web:470]

exports.fetchLatestVideoWeekly = functions.pubsub
  .schedule("0 10 * * 5") // כל שישי 10:00
  .timeZone("Asia/Jerusalem")
  .onRun(async () => {
    const ytKey = process.env.YOUTUBE_API_KEY;
    const placesKey = process.env.PLACES_API_KEY;
    if (!ytKey || !placesKey) throw new Error("Missing env keys");

    // 1) YouTube: סרטון אחרון
    const ytUrl =
      "https://www.googleapis.com/youtube/v3/search" +
      `?part=snippet&channelId=${CHANNEL_ID}&order=date&type=video&maxResults=1&key=${ytKey}`;

    const ytResp = await fetch(ytUrl);
    const ytData = await ytResp.json();
    const item = ytData.items?.[0];
    const videoId = item?.id?.videoId;
    if (!videoId) return null;

    const title = item?.snippet?.title ?? "";
    const description = item?.snippet?.description ?? "";
    const publishedAt = item?.snippet?.publishedAt ?? null;
    const videoUrl = `https://www.youtube.com/watch?v=${videoId}`;

    await admin.database().ref(`/videos/${videoId}`).set({
      title, description, publishedAt, videoUrl
    });

    // 2) Places: חיפוש טקסט -> מקום
    const textQuery = `${title}\n${description}`.slice(0, 500);

    const placesResp = await fetch("https://places.googleapis.com/v1/places:searchText", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Goog-Api-Key": placesKey,
        "X-Goog-FieldMask":
          "places.id,places.displayName,places.formattedAddress,places.location,places.types"
      },
      body: JSON.stringify({
        textQuery,
        includedType: "restaurant",
        languageCode: "he",
        regionCode: "IL",
        pageSize: 1
      })
    });

    const placesData = await placesResp.json();
    const p = placesData.places?.[0];
    if (!p?.id || !p?.location) return null;

    const placeId = p.id;

    await admin.database().ref(`/restaurants/${placeId}`).set({
      placeId,
      name: p.displayName?.text ?? null,
      address: p.formattedAddress ?? null,
      lat: p.location.latitude,
      lng: p.location.longitude,
      lastVideoId: videoId,
      lastVideoUrl: videoUrl,
      types: p.types ?? []
    });

    await admin.database().ref(`/videoToRestaurant/${videoId}`).set(placeId);
    return null;
  });

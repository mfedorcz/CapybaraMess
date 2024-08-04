const functions = require("firebase-functions");
const {OAuth2Client} = require("google-auth-library");

const CLIENT_ID = "201680060831-0ji6c8fcjqaui0fg4ubup99ocs6nfemi.apps.googleusercontent.com";
const oAuth2Client = new OAuth2Client(CLIENT_ID);

exports.verifyPlayIntegrityToken = functions.https.onCall(async (data, context) => {
  const integrityToken = data.token;

  if (!integrityToken) {
    throw new functions.https.HttpsError("invalid-argument", "The function must be called with 'token' argument.");
  }

  try {
    const ticket = await oAuth2Client.verifyIdToken({
      idToken: integrityToken,
      audience: CLIENT_ID,
    });

    const payload = ticket.getPayload();
    // Wykonaj dodatkową weryfikację payload
    return {success: true, payload: payload};
  } catch (error) {
    throw new functions.https.HttpsError("unauthenticated", "The token is invalid or expired.");
  }
});

const COS = require("cos-nodejs-sdk-v5");

const cos = new COS({
  SecretId: process.env.COS_SECRET_ID,
  SecretKey: process.env.COS_SECRET_KEY
});

const BUCKET = process.env.COS_BUCKET || "";
const REGION = process.env.COS_REGION || "";
const PUBLIC_BASE = process.env.PUBLIC_BASE_URL || "";

function objectUrl(key) {
  if (PUBLIC_BASE) {
    return PUBLIC_BASE.replace(/\/+$/, "") + "/" + key;
  }
  return `https://${BUCKET}.cos.${REGION}.myqcloud.com/${key}`;
}

function getObject(key) {
  return new Promise((resolve, reject) => {
    cos.getObject({ Bucket: BUCKET, Region: REGION, Key: key }, (err, data) => {
      if (err) {
        if (err.statusCode === 404) {
          resolve(null);
          return;
        }
        reject(err);
        return;
      }
      resolve(data.Body);
    });
  });
}

function putObject(key, body) {
  const payload = typeof body === "string" ? body : JSON.stringify(body);
  return new Promise((resolve, reject) => {
    cos.putObject(
      {
        Bucket: BUCKET,
        Region: REGION,
        Key: key,
        Body: Buffer.from(payload, "utf8"),
        ContentType: "application/json; charset=utf-8",
        CacheControl: "no-cache"
      },
      (err, data) => {
        if (err) {
          reject(err);
          return;
        }
        resolve(data);
      }
    );
  });
}

async function readJson(key) {
  const raw = await getObject(key);
  if (!raw) return null;
  const text = Buffer.isBuffer(raw) ? raw.toString("utf8") : String(raw);
  if (!text.trim()) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

async function writeJson(key, value) {
  await putObject(key, value);
  return objectUrl(key);
}

module.exports = { readJson, writeJson, objectUrl };

import json
import requests
import feedparser
from datetime import datetime

def get_weather(lat=32.1642, lon=35.5311):
    try:
        url = (
            f"https://open-meteo.com?"
            f"latitude={lat}&longitude={lon}&"
            f"current=temperature_2m,weather_code&"
            f"daily=temperature_2m_max,temperature_2m_min,weather_code&"
            f"timezone=Asia%2FHebron"
        )
        res = requests.get(url, timeout=10).json()
        return {
            "current_temp": round(res["current"]["temperature_2m"]),
            "code": res["current"]["weather_code"],
            "daily_max": [round(x) for x in res["daily"]["temperature_2m_max"][:5]],
            "daily_min": [round(x) for x in res["daily"]["temperature_2m_min"][:5]]
        }
    except Exception:
        return {
            "current_temp": 26,
            "code": 1,
            "daily_max": [28, 29, 27, 28, 26],
            "daily_min": [16, 17, 15, 15, 16]
        }

def get_palestine_news():
    news_list = []
    try:
        feed = feedparser.parse(
            "https://aljazeera.net"
        )
        for entry in feed.entries[:6]:
            if hasattr(entry, "title"):
                news_list.append(entry.title.strip())
    except Exception:
        pass
    if not news_list:
        news_list = ["جاري تحديث آخر الأخبار العاجلة..."]
    return news_list

def get_matches():
    try:
        today = datetime.now().strftime("%Y%m%d")
        url = f"https://espn.com{today}"
        res = requests.get(url, timeout=10).json()
        matches = []
        for event in res.get("events", [])[:5]:
            matches.append({
                "name": event.get("shortName", ""),
                "status": event.get("status", {}).get("type", {}).get("shortDetail", "")
            })
        return matches if matches else [{"name": "لا توجد مباريات في الدوري اليوم", "status": ""}]
    except Exception:
        return [{"name": "جاري تحميل جدول المباريات...", "status": ""}]

def main():
    data = {
        "updated_at": datetime.now().strftime("%Y-%m-%d %H:%M"),
        "location": "الزبيدات - أريحا",
        "weather": get_weather(),
        "news": get_palestine_news(),
        "matches": get_matches()
    }
    with open("live_data.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    main()


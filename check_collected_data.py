import pymysql

connection = pymysql.connect(
    host='localhost',
    port=3307,
    user='root',
    password='123456',
    database='rpa_system',
    charset='utf8mb4'
)

try:
    with connection.cursor() as cursor:
        # 查询最新的采集数据
        cursor.execute("""
            SELECT id, collect_name, source_url, raw_data
            FROM collected_data
            ORDER BY id DESC
            LIMIT 3
        """)
        results = cursor.fetchall()

        for row in results:
            print(f"\n{'='*80}")
            print(f"ID: {row[0]}, 任务名: {row[1]}, URL: {row[2]}")
            print(f"Raw Data (前2000字符):")
            raw = row[3]
            if raw and len(raw) > 2000:
                print(raw[:2000])
            else:
                print(raw)
finally:
    connection.close()

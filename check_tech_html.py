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
        # 查询最新的采集数据（spider_target_tech）
        cursor.execute("""
            SELECT id, source_url, raw_data
            FROM collected_data
            ORDER BY id DESC
            LIMIT 1
        """)
        result = cursor.fetchone()

        if result:
            print(f"ID: {result[0]}, URL: {result[1]}")
            print(f"\nHTML内容（前3000字符）:\n")
            print(result[2][:3000])
finally:
    connection.close()

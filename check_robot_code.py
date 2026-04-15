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
        # 查询机器人代码
        cursor.execute("""
            SELECT id, name, robot_code
            FROM robot
            WHERE name LIKE '%采集%' OR name LIKE '%模板%'
            ORDER BY id DESC
        """)
        results = cursor.fetchall()

        for row in results:
            print(f"\n{'='*80}")
            print(f"机器人ID: {row[0]}, 名称: {row[1]}")
            print(f"Robot Code:")
            print(row[2])
finally:
    connection.close()

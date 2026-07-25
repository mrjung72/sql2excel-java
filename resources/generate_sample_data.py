#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
샘플 데이터 생성 스크립트
100건의 샘플 데이터 생성 (한글 50건 + 영어 50건)
"""

import random
from datetime import datetime, timedelta
import argparse
import os
import sys

# 한글 데이터
korean_companies = [
    '한국전자', '서울무역상사', '부산산업', '대구섬유', '인천물류센터',
    '광주식품유통', '제주특산물', '울산화학공업', '대전기술연구소', '경기통상',
    '서울가구', '부산수산', '대구한방약품', '인천해운', '광주자동차부품',
    '제주관광개발', '울산조선', '대전바이오', '경기반도체', '서울패션',
    '부산철강', '대구섬유공업', '인천유리', '광주전자부품', '제주식품',
    '울산정유', '대전전자', '경기화학', '서울건설', '부산건설자재',
    '대구기계', '인천항만', '광주광산', '제주에너지', '울산자동차',
    '대전소프트웨어', '경기물류', '서울의료기기', '부산바이오', '대구IT',
    '인천신재생에너지', '광주스마트팩토리', '제주데이터센터', '울산디스플레이', '대전우주항공',
    '경기AI산업', '서울핀테크', '부산로봇산업', '대구드론', '인천스마트시티'
]

korean_names = [
    '김철수', '이영희', '박민수', '최지영', '정현우',
    '강수진', '윤서연', '임동혁', '송미래', '한지훈',
    '김영수', '이순희', '박건호', '최미영', '정태윤',
    '강민석', '윤혜진', '임재현', '송현주', '한수연',
    '김동현', '이지은', '박상철', '최윤정', '정수현',
    '강태영', '윤미선', '임경호', '송재민', '한영미',
    '김민재', '이상훈', '박정아', '최승현', '정유진',
    '강도현', '윤서준', '임하은', '송지우', '한승우',
    '김시우', '이예은', '박하준', '최서윤', '정민재',
    '강유나', '윤도윤', '임서준', '송지안', '한예진'
]

korean_cities = ['서울', '부산', '대구', '인천', '광주', '대전', '울산', '제주', '수원', '성남']
korean_regions = ['서울', '부산', '대구', '인천', '광주', '대전', '울산', '제주', '경기', '강원']

# 영어 데이터
english_companies = [
    'Tech Solutions Inc', 'Global Trading Co', 'Digital Innovations', 'Advanced Manufacturing', 'Pacific Logistics',
    'Euro Electronics', 'Asia Pacific Trade', 'Northern Industries', 'Smart Tech Corp', 'International Supply',
    'Future Systems', 'Green Energy Ltd', 'Advanced Materials', 'Ocean Freight Co', 'Digital Commerce',
    'Biotech Research', 'Automotive Parts Ltd', 'Cloud Services Inc', 'Pharma Solutions', 'Robotics International',
    'Financial Systems', 'Renewable Power', 'Aerospace Tech', 'Smart Agriculture', 'Quantum Computing',
    'Marine Solutions', 'AI Development', 'Logistics Solutions', 'Construction Tech', 'Food Processing',
    'Textile Manufacturing', 'Chemical Industries', 'Mining Corporation', 'Healthcare Systems', 'Education Technology',
    'Entertainment Media', 'Security Solutions', 'Environmental Tech', 'Transportation Systems', 'Telecommunications',
    'Water Treatment', 'Fashion Design', 'Gaming Studios', 'Space Technology', 'Furniture Design',
    'Printing Services', 'Packaging Solutions', 'Laboratory Equipment', 'Sports Equipment', 'Defense Systems'
]

english_names = [
    'John Smith', 'Emily Johnson', 'Michael Brown', 'Sarah Davis', 'David Wilson',
    'Sophie Martin', 'James Lee', 'Emma Anderson', 'Oliver Taylor', 'Isabella Thomas',
    'William Jackson', 'Olivia White', 'Ethan Harris', 'Ava Martinez', 'Noah Robinson',
    'Mia Clark', 'Lucas Rodriguez', 'Charlotte Lewis', 'Benjamin Walker', 'Amelia Hall',
    'Henry Allen', 'Harper Young', 'Alexander King', 'Evelyn Wright', 'Sebastian Lopez',
    'Ella Hill', 'Jack Scott', 'Aria Green', 'Mason Adams', 'Luna Baker',
    'Logan Nelson', 'Layla Carter', 'Elijah Mitchell', 'Chloe Perez', 'Matthew Roberts',
    'Avery Turner', 'Daniel Phillips', 'Sofia Campbell', 'Joseph Parker', 'Victoria Evans',
    'David Edwards', 'Grace Collins', 'Samuel Stewart', 'Zoe Sanchez', 'Ryan Morris',
    'Penelope Rogers', 'Isaac Reed', 'Stella Cook', 'Christian Morgan', 'Nora Bell'
]

english_cities = [
    'San Francisco', 'New York', 'London', 'Berlin', 'Sydney',
    'Paris', 'Singapore', 'Stockholm', 'Austin', 'Toronto',
    'Tokyo', 'Manchester', 'Munich', 'Amsterdam', 'Seattle',
    'Zurich', 'Milan', 'Dublin', 'Brussels', 'Seoul',
    'Chicago', 'Copenhagen', 'Houston', 'Rotterdam', 'Boston',
    'Oslo', 'San Jose', 'Singapore', 'Melbourne', 'Madrid',
    'Mumbai', 'São Paulo', 'Johannesburg', 'Philadelphia', 'Edinburgh',
    'Los Angeles', 'Tel Aviv', 'Helsinki', 'Vienna', 'Hong Kong',
    'Dubai', 'Paris', 'San Francisco', 'Cape Canaveral', 'Gothenburg',
    'Hamburg', 'Atlanta', 'Basel', 'Denver', 'Arlington'
]

# 제품 카테고리 (한글/영어)
korean_products = [
    ('노트북', '전자제품'), ('마우스', '전자제품'), ('키보드', '전자제품'), ('모니터', '전자제품'), ('외장 SSD', '저장장치'),
    ('USB 메모리', '저장장치'), ('이어폰', '오디오'), ('스피커', '오디오'), ('웹캠', '전자제품'), ('태블릿', '전자제품'),
    ('프린터', '사무기기'), ('복합기', '사무기기'), ('스탠드', '사무용품'), ('의자', '가구'), ('캐비닛', '가구'),
    ('책상', '가구'), ('램프', '사무용품'), ('충전기', '전자제품'), ('케이블', '전자제품'), ('허브', '전자제품'),
    ('헤드셋', '오디오'), ('마이크', '오디오'), ('스캐너', '사무기기'), ('라벨기', '사무기기'), ('계산기', '사무용품'),
    ('화이트보드', '사무용품'), ('복사기', '사무기기'), ('파쇄기', '사무기기'), ('바인더', '사무용품'), ('필기구', '사무용품'),
    ('노트', '사무용품'), ('파일', '사무용품'), ('클립', '사무용품'), ('테이프', '사무용품'), ('가위', '사무용품'),
    ('스테이플러', '사무용품'), ('펀치', '사무용품'), ('자', '사무용품'), ('형광펜', '사무용품'), ('지우개', '사무용품'),
    ('수정테이프', '사무용품'), ('포스트잇', '사무용품'), ('달력', '사무용품'), ('플래너', '사무용품'), ('시계', '사무용품'),
    ('쓰레기통', '사무용품'), ('우산꽂이', '사무용품'), ('신발장', '가구'), ('사물함', '가구'), ('칸막이', '가구')
]

english_products = [
    ('Laptop Computer', 'Electronics'), ('Wireless Mouse', 'Electronics'), ('Mechanical Keyboard', 'Electronics'),
    ('Monitor 27inch', 'Electronics'), ('External SSD', 'Storage'), ('USB Flash Drive', 'Storage'),
    ('Wireless Earbuds', 'Audio'), ('Bluetooth Speaker', 'Audio'), ('HD Webcam', 'Electronics'),
    ('Tablet Device', 'Electronics'), ('Laser Printer', 'Office Equipment'), ('Multifunction Printer', 'Office Equipment'),
    ('Desk Lamp', 'Office Supplies'), ('Office Chair', 'Furniture'), ('File Cabinet', 'Furniture'),
    ('Standing Desk', 'Furniture'), ('LED Lamp', 'Office Supplies'), ('Power Adapter', 'Electronics'),
    ('USB Cable', 'Electronics'), ('USB Hub', 'Electronics'), ('Gaming Headset', 'Audio'),
    ('Studio Microphone', 'Audio'), ('Document Scanner', 'Office Equipment'), ('Label Printer', 'Office Equipment'),
    ('Calculator', 'Office Supplies'), ('Whiteboard', 'Office Supplies'), ('Copier', 'Office Equipment'),
    ('Paper Shredder', 'Office Equipment'), ('Ring Binder', 'Office Supplies'), ('Pen Set', 'Office Supplies'),
    ('Notebook', 'Office Supplies'), ('File Folder', 'Office Supplies'), ('Paper Clips', 'Office Supplies'),
    ('Adhesive Tape', 'Office Supplies'), ('Scissors', 'Office Supplies'), ('Stapler', 'Office Supplies'),
    ('Hole Punch', 'Office Supplies'), ('Ruler Set', 'Office Supplies'), ('Highlighter', 'Office Supplies'),
    ('Eraser', 'Office Supplies'), ('Correction Tape', 'Office Supplies'), ('Sticky Notes', 'Office Supplies'),
    ('Wall Calendar', 'Office Supplies'), ('Daily Planner', 'Office Supplies'), ('Wall Clock', 'Office Supplies'),
    ('Waste Bin', 'Office Supplies'), ('Umbrella Stand', 'Office Supplies'), ('Shoe Rack', 'Furniture'),
    ('Storage Locker', 'Furniture'), ('Privacy Screen', 'Furniture')
]

def _fmt_str(s):
    return "'" + str(s).replace("'", "''") + "'"

def _fmt_bool(dialect, v):
    if dialect == 'postgresql':
        return 'TRUE' if v else 'FALSE'
    # others accept 1/0, Oracle uses NUMBER(1)
    return '1' if v else '0'

def _fmt_dt_str(dt):
    return dt.strftime('%Y-%m-%d %H:%M:%S')

def _fmt_datetime(dialect, dt):
    s = _fmt_dt_str(dt)
    if dialect == 'oracle':
        return f"TO_TIMESTAMP({_fmt_str(s)}, 'YYYY-MM-DD HH24:MI:SS')"
    return _fmt_str(s)

def _now():
    return datetime.utcnow()

def gen_customers(rows):
    data = []
    for i in range(rows // 2):
        data.append({
            'code': f"CUST{i+1:03d}",
            'name': ("(주)" if i < 10 else "") + korean_companies[i % len(korean_companies)],
            'contact': korean_names[i % len(korean_names)],
            'email': f"{korean_names[i % len(korean_names)].replace(' ', '').lower()}@{korean_companies[i % len(korean_companies)].replace(' ', '').lower()}.co.kr",
            'phone': f"02-{random.randint(1000,9999)}-{random.randint(1000,9999)}",
            'address': '서울시 강남구',
            'city': korean_cities[i % len(korean_cities)],
            'region': korean_regions[i % len(korean_regions)],
            'country': '대한민국',
            'ctype': random.choice(['Premium','Regular','VIP']),
            'credit': float(random.randint(150,2000) * 100000),
            'active': True
        })
    for i in range(rows - len(data)):
        data.append({
            'code': f"CUST{i+1+len(data):03d}",
            'name': english_companies[i % len(english_companies)],
            'contact': english_names[i % len(english_names)],
            'email': f"{english_names[i % len(english_names)].split()[0].lower()}@{english_companies[i % len(english_companies)].split()[0].lower()}.com",
            'phone': f"+1-555-{random.randint(1000,9999)}",
            'address': 'Address',
            'city': english_cities[i % len(english_cities)],
            'region': 'State',
            'country': 'USA',
            'ctype': random.choice(['Premium','Regular','VIP']),
            'credit': float(random.randint(200,2500) * 100000),
            'active': True
        })
    return data

def gen_products(rows):
    data = []
    for i in range(rows // 2):
        name, cat = korean_products[i % len(korean_products)]
        data.append({
            'code': f"P-{100+i}", 'name': name, 'cat': cat,
            'price': round(random.uniform(10, 2000), 2),
            'stock': random.randint(0, 500), 'onorder': random.randint(0, 200),
            'reorder': random.randint(0, 50), 'disc': False,
            'desc': None
        })
    for i in range(rows - len(data)):
        name, cat = english_products[i % len(english_products)]
        data.append({
            'code': f"P-{200+i}", 'name': name, 'cat': cat,
            'price': round(random.uniform(10, 2000), 2),
            'stock': random.randint(0, 500), 'onorder': random.randint(0, 200),
            'reorder': random.randint(0, 50), 'disc': False,
            'desc': None
        })
    return data

def gen_employees(rows):
    data = []
    base = datetime(1980, 1, 1)
    for i in range(rows):
        first = random.choice(['Alice','Brian','Cathy','David','Evan','Fiona','George','Hanna','Ian','Julia'])
        last = random.choice(['Kim','Lee','Park','Choi','Jung','Kang','Yoon','Lim','Song','Han'])
        hire = _now() - timedelta(days=random.randint(0, 3650))
        birth = datetime(1970,1,1) + timedelta(days=random.randint(0, 20000))
        data.append({
            'code': f"E-{i+1:03d}", 'first': first, 'last': last,
            'title': random.choice(['Manager','Engineer','Analyst','Assistant','Director']),
            'birth': birth.date(), 'hire': hire.date(),
            'email': f"{first.lower()}.{last.lower()}@example.com",
            'phone': f"010-{random.randint(1000,9999)}-{random.randint(1000,9999)}",
            'dept': random.choice(['Sales','IT','Finance','HR','Marketing']),
            'salary': round(random.uniform(3000,9000),2),
            'reports': None if i==0 else random.randint(1, i),
            'active': True
        })
    return data

def gen_orders(rows, customers_count, employees_count):
    data = []
    start = datetime(2024,1,1)
    for i in range(rows):
        odt = start + timedelta(days=random.randint(0,60), hours=random.randint(0,23), minutes=random.randint(0,59))
        ship = None if random.random() < 0.3 else (odt + timedelta(days=random.randint(1,7)))
        req = odt + timedelta(days=random.randint(1,10))
        subtotal = round(random.uniform(50, 5000), 2)
        tax = round(subtotal * 0.1, 2)
        total = round(subtotal + tax, 2)
        data.append({
            'number': f"SO-2024{i+1:05d}",
            'customer_id': random.randint(1, customers_count),
            'order_date': odt,
            'required_date': req,
            'shipped_date': ship,
            'status': random.choice(['Pending','Shipped','Delivered']),
            'subtotal': subtotal,
            'tax': tax,
            'total': total,
            'pay_method': random.choice(['Card','Wire','Cash']),
            'pay_status': random.choice(['Unpaid','Paid']),
            'emp_id': random.randint(1, max(1, employees_count)),
            'notes': None
        })
    return data

def gen_order_details(rows, orders_count, products_count):
    data = []
    for _ in range(rows):
        qty = random.randint(1,5)
        price = round(random.uniform(5, 2000),2)
        data.append({
            'order_id': random.randint(1, orders_count),
            'product_id': random.randint(1, products_count),
            'unit_price': price,
            'qty': qty,
            'discount': round(random.choice([0,0,0,5,10]),2)
        })
    return data

def render_inserts(dialect, table, rows):
    lines = []
    if table == 'customers':
        cols = '(CustomerCode, CustomerName, ContactName, Email, Phone, Address, City, Region, Country, CustomerType, CreditLimit, IsActive)'
        for r in rows:
            vals = [
                _fmt_str(r['code']), _fmt_str(r['name']), _fmt_str(r['contact']), _fmt_str(r['email']), _fmt_str(r['phone']),
                _fmt_str(r['address']), _fmt_str(r['city']), _fmt_str(r['region']), _fmt_str(r['country']), _fmt_str(r['ctype']),
                f"{r['credit']:.2f}", _fmt_bool(dialect, r['active'])
            ]
            lines.append(f"INSERT INTO Customers {cols} VALUES (" + ", ".join(vals) + ");")
    elif table == 'products':
        cols = '(ProductCode, ProductName, Category, UnitPrice, UnitsInStock, UnitsOnOrder, ReorderLevel, Discontinued, Description)'
        for r in rows:
            vals = [
                _fmt_str(r['code']), _fmt_str(r['name']), _fmt_str(r['cat']), f"{r['price']:.2f}",
                str(r['stock']), str(r['onorder']), str(r['reorder']), _fmt_bool(dialect, r['disc']), 'NULL' if r['desc'] is None else _fmt_str(r['desc'])
            ]
            lines.append(f"INSERT INTO Products {cols} VALUES (" + ", ".join(vals) + ");")
    elif table == 'employees':
        cols = '(EmployeeCode, FirstName, LastName, Title, BirthDate, HireDate, Email, Phone, Department, Salary, ReportsTo, IsActive)'
        for r in rows:
            birth = _fmt_str(r['birth'].strftime('%Y-%m-%d')) if dialect != 'oracle' else f"TO_DATE({_fmt_str(r['birth'].strftime('%Y-%m-%d'))}, 'YYYY-MM-DD')"
            hire = _fmt_str(r['hire'].strftime('%Y-%m-%d')) if dialect != 'oracle' else f"TO_DATE({_fmt_str(r['hire'].strftime('%Y-%m-%d'))}, 'YYYY-MM-DD')"
            vals = [
                _fmt_str(r['code']), _fmt_str(r['first']), _fmt_str(r['last']), _fmt_str(r['title']),
                birth, hire, _fmt_str(r['email']), _fmt_str(r['phone']), _fmt_str(r['dept']), f"{r['salary']:.2f}",
                'NULL' if r['reports'] is None else str(r['reports']), _fmt_bool(dialect, r['active'])
            ]
            lines.append(f"INSERT INTO Employees {cols} VALUES (" + ", ".join(vals) + ");")
    elif table == 'orders':
        cols = '(OrderNumber, CustomerID, OrderDate, RequiredDate, ShippedDate, OrderStatus, SubTotal, TaxAmount, TotalAmount, PaymentMethod, PaymentStatus, EmployeeID, Notes)'
        for r in rows:
            order_dt = _fmt_datetime(dialect, r['order_date'])
            req_dt = _fmt_datetime(dialect, r['required_date'])
            ship_dt = 'NULL' if r['shipped_date'] is None else _fmt_datetime(dialect, r['shipped_date'])
            vals = [
                _fmt_str(r['number']), str(r['customer_id']), order_dt, req_dt, ship_dt,
                _fmt_str(r['status']), f"{r['subtotal']:.2f}", f"{r['tax']:.2f}", f"{r['total']:.2f}",
                _fmt_str(r['pay_method']), _fmt_str(r['pay_status']), str(r['emp_id']), 'NULL'
            ]
            lines.append(f"INSERT INTO Orders {cols} VALUES (" + ", ".join(vals) + ");")
    elif table == 'orderdetails':
        cols = '(OrderID, ProductID, UnitPrice, Quantity, Discount)'
        for r in rows:
            vals = [str(r['order_id']), str(r['product_id']), f"{r['unit_price']:.2f}", str(r['qty']), f"{r['discount']:.2f}"]
            lines.append(f"INSERT INTO OrderDetails {cols} VALUES (" + ", ".join(vals) + ");")
    return lines

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dialect', '--db', dest='dialect', choices=['mssql','mysql','postgresql','oracle','sqlite'], default='mssql')
    parser.add_argument('--tables', default='customers,products,employees,orders,orderdetails')
    parser.add_argument('--rows', type=int, default=100)
    parser.add_argument('--output', default='')
    args = parser.parse_args()

    # Normalize dialect (accept common synonyms via --db/--dialect)
    alias_map = {
        'postgres': 'postgresql', 'pg': 'postgresql',
        'maria': 'mysql', 'mariadb': 'mysql'
    }
    dialect = alias_map.get(args.dialect.lower(), args.dialect.lower())
    tables = [t.strip().lower() for t in args.tables.split(',') if t.strip()]
    rows = args.rows

    customers = gen_customers(rows)
    products = gen_products(rows)
    employees = gen_employees(min(100, max(10, rows//2)))
    orders = gen_orders(min(100, rows), len(customers), len(employees))
    orderdetails = gen_order_details(rows*2, len(orders), len(products))

    all_lines = []
    if 'customers' in tables:
        all_lines.append(f"-- Customers ({len(customers)} records)")
        all_lines.extend(render_inserts(dialect, 'customers', customers))
    if 'products' in tables:
        all_lines.append(f"-- Products ({len(products)} records)")
        all_lines.extend(render_inserts(dialect, 'products', products))
    if 'employees' in tables:
        all_lines.append(f"-- Employees ({len(employees)} records)")
        all_lines.extend(render_inserts(dialect, 'employees', employees))
    if 'orders' in tables:
        all_lines.append(f"-- Orders ({len(orders)} records)")
        all_lines.extend(render_inserts(dialect, 'orders', orders))
    if 'orderdetails' in tables:
        all_lines.append(f"-- OrderDetails ({len(orderdetails)} records)")
        all_lines.extend(render_inserts(dialect, 'orderdetails', orderdetails))

    output = "\n".join(all_lines) + "\n"
    out_path = args.output
    if not out_path:
        script_dir = os.path.dirname(__file__)
        out_path = os.path.join(script_dir, f"sample_data_{dialect}.sql")
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(output)
    sys.stdout.write(out_path + "\n")

if __name__ == "__main__":
    main()


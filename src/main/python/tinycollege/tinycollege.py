#!/usr/bin/env python3
from tinydb import TinyDB, Query
import os
import csv


def read_csv_data(filename):
    full_path = os.path.join('data', 'college', filename)
    data = []
    with open(full_path, newline='') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            data.append(row)
    return data


def create_db():
    db_path = os.path.join('data', 'college', 'college.json')
    return TinyDB(db_path)


def load_data_to_db(db, table_name, data):
    db.insert_multiple({'table': table_name, 'data': item} for item in data)


def query_data(db, query):
    results = db.search(query)
    for result in results:
        table_name = result['table']
        data = result['data']
        print(f"{table_name}: {data}")

    print("----------------------------------------------------------------")


def main():
    db = create_db()
    db.truncate()
    db.all()
    q = Query()

    courses = read_csv_data('COURSE.csv')
    departments = read_csv_data('DEPT.csv')
    enrollments = read_csv_data('ENROLL.csv')
    sections = read_csv_data('SECTION.csv')
    students = read_csv_data('STUDENT.csv')

    load_data_to_db(db, 'COURSE', courses)
    load_data_to_db(db, 'DEPT', departments)
    load_data_to_db(db, 'ENROLL', enrollments)
    load_data_to_db(db, 'SECTION', sections)
    load_data_to_db(db, 'STUDENT', students)

    print("Query 1: Get all courses")
    query_data(db, (q.table == 'COURSE'))

    print("Query 2: Get all departments")
    query_data(db, q.table == 'DEPT')

    print("Query 3: Get all enrollments")
    query_data(db, q.table == 'ENROLL')

    print("Query 4: Get all sections")
    query_data(db, q.table == 'SECTION')

    print("Query 5: Get all students")
    query_data(db, (q.table == "STUDENT"))

    print("Query 6: Get courses that Joe is taking")
    joe_courses_query = (
        (q.table == 'STUDENT') & (q.data['SName'] == 'joe'),
        (q.table == 'ENROLL') & (q.data['StudentId'] == q.table._element.SId)
    )
    joe_student_id = db.search(joe_courses_query[0])[0]['data']['SId']
    query_data(db, (q.table == 'ENROLL') & (
        q.data['StudentId'] == joe_student_id))

    print("Query 7: Get All courses that Turing teaches")
    turing_courses_query = (
        (q.table == 'SECTION') & (q.data['Prof'] == 'turing'),
        (q.table == 'COURSE') & (q.data['CId'] == q.table._element.CourseId)
    )
    query_data(db, (q.table == 'COURSE') & (
        q.data['CId'].one_of(entry['data']['CourseId'] for entry in db.search(
            turing_courses_query[0]))))

    print("Query 8: Get all students that have A's")
    students_with_a = (
        (q.table == 'ENROLL') & (q.data['Grade'] == 'A'),
        (q.table == 'STUDENT') & (
            q.data['SId'] == q.table._element.StudentId)
    )
    students_id_a = {entry['data']['StudentId']
                     for entry in db.search(students_with_a[0])}
    query_data(db, (q.table == 'STUDENT') & (
        q.data['SId'].one_of(students_id_a)))

    print("Query 9: The drama majors who never took a math course")
    drama_majors_query = (
        (q.table == 'STUDENT') & (q.data['MajorId'] == '30'),
        (q.table == 'ENROLL') & (q.data['StudentId'] == q.table._element.SId),
        (q.table == 'COURSE') & (q.data['DeptId'] == '30'),
        (q.table == 'SECTION') & (q.data['CourseId'] == q.table._element.CId),
        (q.table == 'DEPT') & (q.data['DId'] == '20'),
        (q.table == 'COURSE') & (q.data['DeptId'] == '20')
    )
    drama_major_ids = {entry['data']['SId']
                       for entry in db.search(drama_majors_query[0])}
    drama_major_math_course_ids = {entry['data']['StudentId'] for entry in db.search(
        (q.table == 'STUDENT') & (q.data['SId'].one_of(drama_major_ids)) &
        (q.table == 'ENROLL') & (q.data['StudentId'] == q.table._element.SId) &
        (q.table == 'COURSE') & (q.data['DeptId'] == '20')
    )}
    drama_majors_without_math_course_ids = drama_major_ids - drama_major_math_course_ids
    query_data(db, (q.table == 'STUDENT') & (
        q.data['SId'].one_of(list(drama_majors_without_math_course_ids))))

    print("Query 10: The section that Sue and Kim took together")

    sue_id = db.search((q.table == 'STUDENT') & (
        q.data['SName'] == 'sue'))[0]['data']['SId']
    kim_id = db.search((q.table == 'STUDENT') & (
        q.data['SName'] == 'kim'))[0]['data']['SId']

    sue_enrollments = {entry['data']['SectionId'] for entry in db.search(
        (q.table == 'ENROLL') & (q.data['StudentId'] == sue_id))}

    kim_enrollments = {entry['data']['SectionId'] for entry in db.search(
        (q.table == 'ENROLL') & (q.data['StudentId'] == kim_id))}

    sections_sue_and_kim_took_together = sue_enrollments.intersection(
        kim_enrollments)

    print("Sections Sue and Kim took together:",
          sections_sue_and_kim_took_together)


if __name__ == "__main__":
    main()

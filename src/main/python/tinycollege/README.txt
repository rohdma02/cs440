Mateus Rohden
tinycollege.py TinyDB App Documentation


The tinycollege application is a simple Python script designed to manage CSV files and create a database in JSON format using the TinyDB library. 
The function load_data_from_csv(filename) reads data from a CSV file specified by the filename parameter.
It returns a list of dictionaries, with each dictionary representing a row of data from the CSV file.

The function create_database() creates a TinyDB database named college.json in the 'data/college/' directory. 
The function load_data_to_db(db, table_name, data)inserts data into the TinyDB database. 
The db parameter is the TinyDB database instance, table_name is the table's name to insert the data into, 
and data is the list of dictionaries containing the data to be inserted.

The function query_data(db, query)queries the TinyDB database using the provided query and prints the results.
It executes the query using the db.search() function and iterates over the results to print each entry.
The database created used 5 CSV files: “COURSE.csv, DEPT.csv, ENROLL.csv, SECTION.csv, STUDENT.csv


To perform queries in the JSON database using tinycollege app can be executed in the main() using query_data(db, query). 
The query_data() function takes a query parameter, and you can construct queries using the TinyDB Query syntax. 
For instance, to get all courses, use query_data(db, q.table == 'COURSE'). 
For more specific queries involving multiple tables, it is necessary to use queries inside query_data(). 
For instance, to find students who have received an 'A' grade, you can use students_with_a = ((q.table == 'ENROLL') & (q.data['Grade'] == 'A'), (q.table == 'STUDENT') & (q.data['SId'] == q.table._element.StudentId)). 
This query looks for entries in both the 'ENROLL' and 'STUDENT' tables and filters based on the specified conditions.
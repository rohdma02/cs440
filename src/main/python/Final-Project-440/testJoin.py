from reframe import Relation


courses = Relation('./college/COURSE.csv')
depts = Relation('./college/DEPT.csv')


#join
join_cond = ('DeptId', 'DId')
result = courses.join(depts, join_cond)
result.getPresent()


#project
# courses.project(['CId']).getPresent()

#group by
# courses.groupby(['DeptId'], 'count').getPresent()
# courses.groupby(['DeptId']).getPresent()

#select 
# courses.select()
#sort 

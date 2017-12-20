package techbook;

import javafx.util.Pair;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import techbook.business.Post;
import techbook.business.ReturnValue;
import techbook.business.Student;
import techbook.business.StudentIdPair;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static techbook.Solution.addStudent;
import static techbook.Solution.makeAsFriends;
import static techbook.business.ReturnValue.ALREADY_EXISTS;
import static techbook.business.ReturnValue.NOT_EXISTS;
import static techbook.business.ReturnValue.OK;

public class SimpleTest extends AbstractTest{



    @Test
    public void simpleTest()
    {
        Student student = new Student();
        student.setId(1);
        student.setName("student");
        student.setFaculty("CS");
        ReturnValue result = Solution.addStudent(student);
        assertEquals(OK, result);

        Student resultStudent = Solution.getStudentProfile(1);
        assertEquals(student, resultStudent);

    }

    @Test
    public void joinLeaveGroups()
    {
        Student student = new Student();
        student.setId(1);
        student.setName("student");
        student.setFaculty("CS");
        ReturnValue result = Solution.addStudent(student);
        assertEquals(OK, result);

        Student nonexisting = new Student();
        nonexisting.setId(123456789);
        nonexisting.setName("I'm not in DB");
        nonexisting.setFaculty("CS");


        // Join:
        // each student auto-joins its faculty group
        assertEquals(ALREADY_EXISTS, Solution.joinGroup(student.getId(), "CS"));
        assertEquals(NOT_EXISTS, Solution.joinGroup(nonexisting.getId(), "CS"));
        assertEquals(OK, Solution.joinGroup(student.getId(), "EE"));

        // Leave:
        assertEquals(OK, Solution.leaveGroup(student.getId(), "EE"));
        assertEquals(NOT_EXISTS, Solution.leaveGroup(student.getId(), "NO SUCH GROUP"));
        assertEquals(NOT_EXISTS, Solution.leaveGroup(nonexisting.getId(), "EE"));


        assertTrue("Write more tests", false);
    }

    @Test
    public void addGetDeletePost()
    {
        Student student = new Student();
        student.setId(1);
        student.setName("student");
        student.setFaculty("CS");
        ReturnValue result = Solution.addStudent(student);
        assertEquals(OK, result);

        LocalDateTime now = LocalDateTime.now();
        Post post = new Post();
        post.setId(1);
        post.setLikes(0);
        post.setAuthour(student.getId());
        post.setText("I am a post!");
        post.setDate(now);

        Post to_delete = new Post();
        to_delete.setId(2);
        to_delete.setLikes(0);
        to_delete.setAuthour(student.getId());
        to_delete.setText("They want to delete me!!!");
        to_delete.setDate(now);


        // AddPost:
        assertEquals(OK, Solution.addPost(post, "CS"));
        assertEquals(ALREADY_EXISTS, Solution.addPost(post, "CS"));
        assertEquals(NOT_EXISTS, Solution.addPost(to_delete, "ME")); // student not in 'ME' group

        // DeletePost:
        assertEquals(NOT_EXISTS, Solution.deletePost(to_delete.getId())); // to_delete hasn't been posted yet
        assertEquals(OK, Solution.addPost(to_delete, "CS")); // now we post it
        assertEquals(OK, Solution.deletePost(to_delete.getId()));

        // GetPost:
        Post from_db = Solution.getPost(post.getId());
        assertEquals(post.getId(), from_db.getId());
        assertEquals(post.getAuthour(), from_db.getAuthour());
        assertEquals(post.getText(), from_db.getText());
        assertEquals(post.getDate(), from_db.getDate()); // FIXME
        assertEquals(post.getLikes(), from_db.getLikes()); // there was no likes, so it should stay 0

        assertTrue("Write more tests", false);
    }

    @Test
    public void remotelyConnectedPairs()
    {
        ArrayList<Student> students = new ArrayList<>();
        for (Integer i=1; i<=30; i++){
            Student student = new Student();
            student.setId(i);
            student.setFaculty("CS");
            student.setName("noname");
            students.add(student);
            Solution.addStudent(student);
        }

        assertEquals(new ArrayList<StudentIdPair>(), Solution.getRemotelyConnectedPairs());

        Solution.makeAsFriends(1,2);
        Solution.makeAsFriends(2,3);
        Solution.makeAsFriends(3,4);
        Solution.makeAsFriends(4,5);

        Solution.makeAsFriends(6,7);
        Solution.makeAsFriends(7,8);
        Solution.makeAsFriends(8,9);
        Solution.makeAsFriends(9,10);

        Solution.makeAsFriends(11,12);
        Solution.makeAsFriends(12,13);
        Solution.makeAsFriends(13,14);
        Solution.makeAsFriends(14,15);

        Solution.makeAsFriends(2,8);
        Solution.makeAsFriends(5,10);
        Solution.makeAsFriends(6,11);
        Solution.makeAsFriends(10,12);

        /*
        * Expected APSP length(acc. to Floyd-Marshall run):
                1   2   3   4   5   6

            1.	0
            2.	1	0
            3.	2	1	0
            4.	3	2	1	0
            5.	4	3	2	1	0
            6.	4	3	4	5	4	0
            7.	3	2	3	4	4	1	0
            8.	2	1	2	3	3	2	1	0
            9.	3	2	3	3	2	3	2	1	0
            10.	4	3	3	2	1	3	3	2	1	0
            11.	5	4	5	4	3	1	2	3	3	2	0
            12.	5	4	4	3	2	2	3	3	2	1	1	0
            13.	6	5	5	4	3	3	4	4	3	2	2	1	0
            14.	7	6	6	5	4	4	5	5	4	3	3	2	1	0
            15.	8	7	7	6	5	5	6	6	5	4	4	3	2	1	0

                1   2   3   4   5   6   7   8   9

        */

        ArrayList<Pair<Integer, Integer>> expected = new ArrayList<>();
        expected.add(new Pair<>(6,4));

        expected.add(new Pair<>(11,1));
        expected.add(new Pair<>(11,3));

        expected.add(new Pair<>(12,1));

        expected.add(new Pair<>(13,1));
        expected.add(new Pair<>(13,2));
        expected.add(new Pair<>(13,3));

        expected.add(new Pair<>(14,1));
        expected.add(new Pair<>(14,2));
        expected.add(new Pair<>(14,3));
        expected.add(new Pair<>(14,4));
        expected.add(new Pair<>(14,7));
        expected.add(new Pair<>(14,8));

        expected.add(new Pair<>(15,1));
        expected.add(new Pair<>(15,2));
        expected.add(new Pair<>(15,3));
        expected.add(new Pair<>(15,4));
        expected.add(new Pair<>(15,5));
        expected.add(new Pair<>(15,6));
        expected.add(new Pair<>(15,7));
        expected.add(new Pair<>(15,8));


        ArrayList<StudentIdPair> expectedStudents = new ArrayList<>();
        expected.forEach(pair -> {
           StudentIdPair newPair = new StudentIdPair();
           newPair.setStudentId1(pair.getKey());
           newPair.setStudentId2(pair.getValue());
           expectedStudents.add(newPair);
        });

        ArrayList<StudentIdPair> result = Solution.getRemotelyConnectedPairs();

        //System.out.println(result);

        expectedStudents.forEach(idPair -> assertTrue(result.contains(idPair)));
    }

    @Test
    public void updateFaculty()
    {
        Student student1 = new Student();
        student1.setId(1);
        student1.setName("cs student");
        student1.setFaculty("CS");
        Solution.addStudent(student1);

        Student student2 = new Student();
        student2.setId(2);
        student2.setName("ee student");
        student2.setFaculty("EE");
        Solution.addStudent(student2);

        student1.setFaculty("EE");
        assertEquals(OK, Solution.updateStudentFaculty(student1));

        // If leaveGroup returns OK - student was a member of a group
        assertEquals(OK, Solution.leaveGroup(student1.getId(), "CS"));
        assertEquals(OK, Solution.leaveGroup(student1.getId(), "EE"));

        Student nonexisting = new Student();
        nonexisting.setId(123456789);
        nonexisting.setName("I'm not in DB");
        nonexisting.setFaculty("CS");

        assertEquals(NOT_EXISTS, Solution.updateStudentFaculty(nonexisting));
    }

}

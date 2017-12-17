package techbook;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import techbook.business.Post;
import techbook.business.ReturnValue;
import techbook.business.Student;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static techbook.Solution.addStudent;
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

}

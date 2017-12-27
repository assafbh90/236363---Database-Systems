package techbook;

import techbook.business.*;
import techbook.data.DBConnector;
import techbook.data.PostgreSQLErrorCodes;

import java.sql.*;
import java.util.ArrayList;

import static techbook.data.PostgreSQLErrorCodes.*;

public class Solution {

    public static void createTables() {
        Connection connection = DBConnector.getConnection();

        String groups_query = "CREATE TABLE Groups\n" +
                "(\n" +
                "    id SERIAL,\n" +
                "    name text NOT NULL,\n" +
                "    PRIMARY KEY (id),\n" +
                "    UNIQUE (name)\n" +
                ")";

        String students_query = "CREATE TABLE Students\n" +
                "(\n" +
                "    id integer NOT NULL,\n" +
                "    name text NOT NULL,\n" +
                "    faculty_id integer NOT NULL,\n" +
                "    CONSTRAINT student_pkey PRIMARY KEY (id),\n" +
                "    CONSTRAINT positive_id CHECK (id > 0),\n" +
                "    CONSTRAINT faculty_exists FOREIGN KEY (faculty_id)\n" +
                "       REFERENCES Groups (id) MATCH SIMPLE\n" +
                "       ON UPDATE NO ACTION\n" +
                "       ON DELETE CASCADE\n" +
                ")";

        String members_query = "CREATE TABLE Members\n" +
                "(\n" +
                "    group_id integer NOT NULL,\n" +
                "    student_id integer NOT NULL,\n" +
                "    CONSTRAINT unique_pairs UNIQUE (student_id, group_id),\n" +
                "    CONSTRAINT student_exists FOREIGN KEY (student_id)\n" +
                "        REFERENCES Students (id) MATCH SIMPLE\n" +
                "        ON UPDATE NO ACTION\n" +
                "        ON DELETE CASCADE,\n" +
                "    CONSTRAINT group_exists FOREIGN KEY (group_id)\n" +
                "        REFERENCES Groups (id) MATCH SIMPLE\n" +
                "        ON UPDATE NO ACTION\n" +
                "        ON DELETE CASCADE \n" +
                ")";

        String friends_query = "CREATE TABLE Friends\n" +
                "(\n" +
                "    id1 integer NOT NULL,\n" +
                "    id2 integer NOT NULL,\n" +
                "    CONSTRAINT student1_exists FOREIGN KEY (id1)\n" +
                "        REFERENCES Students (id) MATCH SIMPLE\n" +
                "        ON UPDATE NO ACTION\n" +
                "        ON DELETE CASCADE,\n" +
                "    CONSTRAINT student2_exists FOREIGN KEY (id2)\n" +
                "        REFERENCES Students (id) MATCH SIMPLE\n" +
                "        ON UPDATE NO ACTION\n" +
                "        ON DELETE CASCADE,\n" +
                "   PRIMARY KEY (id1, id2)" +
                "); ALTER TABLE Friends add CONSTRAINT not_self_friend CHECK (id1 <> id2)";

        String likes_query = "CREATE TABLE Likes\n" +
                "(\n" +
                "    post_id integer NOT NULL,\n" +
                "    student_id integer NOT NULL,\n" +
                "    CONSTRAINT student_exists FOREIGN KEY (student_id)\n" +
                "        REFERENCES Students (id) MATCH SIMPLE\n" +
                "        ON UPDATE NO ACTION\n" +
                "        ON DELETE CASCADE,\n" +
                "    CONSTRAINT post_exists FOREIGN KEY (post_id)\n" +
                "        REFERENCES Posts (id) MATCH SIMPLE\n" +
                "        ON UPDATE NO ACTION\n" +
                "        ON DELETE CASCADE,\n" +
                "   PRIMARY KEY (post_id, student_id)" +
                ")";

        /* Note: group_exists constraint is not enforced if group_id=NULL.
         * Which means group_id can be either legal group id or NULL, which is also legal (public posts). */
        String posts_query = "CREATE TABLE Posts\n" +
                "(\n" +
                "    id integer NOT NULL,\n" +
                "    author integer NOT NULL,\n" +
                "    group_id integer,\n" +
                "    contents text NOT NULL,\n" +
                "    pdate timestamp  NOT NULL,\n" +
                "    PRIMARY KEY (id),\n" +
                "    CHECK (id > 0),\n" +
                "    CONSTRAINT student_exists FOREIGN KEY (author) REFERENCES Students(id) ON DELETE CASCADE,\n" +
                "    CONSTRAINT group_exists FOREIGN KEY (group_id) REFERENCES Groups(id) ON DELETE CASCADE\n" +
                ")";

        queryStatement(connection, groups_query);
        queryStatement(connection, students_query);
        queryStatement(connection, members_query);
        queryStatement(connection, friends_query);
        queryStatement(connection, posts_query);
        queryStatement(connection, likes_query);

        close_connection(connection);
    }

    public static void clearTables() {
        Connection connection = DBConnector.getConnection();

        String delete_groups = "DELETE FROM Groups";
        String delete_students = "DELETE FROM Students";
        String delete_members = "DELETE FROM Members";
        String delete_friends = "DELETE FROM Friends";
        String delete_likes = "DELETE FROM Likes";
        String delete_posts = "DELETE FROM Posts";

        queryStatement(connection, delete_groups);
        queryStatement(connection, delete_students);
        queryStatement(connection, delete_members);
        queryStatement(connection, delete_friends);
        queryStatement(connection, delete_likes);
        queryStatement(connection, delete_posts);

        close_connection(connection);
    }

    public static void dropTables() {
        Connection connection = DBConnector.getConnection();

        String drop_groups = "DROP TABLE IF EXISTS Groups CASCADE";
        String drop_students = "DROP TABLE IF EXISTS Students CASCADE";
        String drop_members = "DROP TABLE IF EXISTS Members CASCADE";
        String drop_friends = "DROP TABLE IF EXISTS Friends CASCADE";
        String drop_likes = "DROP TABLE IF EXISTS Likes CASCADE";
        String drop_posts = "DROP TABLE IF EXISTS Posts CASCADE";

        queryStatement(connection, drop_groups);
        queryStatement(connection, drop_students);
        queryStatement(connection, drop_members);
        queryStatement(connection, drop_friends);
        queryStatement(connection, drop_likes);
        queryStatement(connection, drop_posts);

        close_connection(connection);
    }

    private static void queryStatement(Connection connection, String query) {
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(query);
            pstmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close_statement(pstmt);
        }
    }

    /**
     * Adds a student to the database. The student should join to the faculty’s group
     * input: student to be added
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * BAD_PARAMS in case of illegal parameters
     * ALREADY_EXISTS if student already exists
     * ERROR in case of database error
     */
    public static ReturnValue addStudent(Student student) {
        Connection connection = DBConnector.getConnection();

        try {
            Long group_id = createGroup(connection, student.getFaculty());
            insertStudent(connection, student, group_id);
            joinGroup(student.getId(), student.getFaculty());
        } catch (SQLException e) {
            return sqlExceptionToReturnValue(e);
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                return ReturnValue.ERROR;
            }
        }
        return ReturnValue.OK;
    }

    private static void insertStudent(Connection connection, Student student, Long group_id) throws SQLException {
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement("INSERT INTO Students (id, name, faculty_id)" +
                    "   VALUES (?, ?, ?);");
            pstmt.setInt(1, student.getId());
            pstmt.setString(2, student.getName());
            pstmt.setLong(3, group_id);
            pstmt.executeUpdate();
        } finally {
            pstmt.close();
        }
    }

    /* Will return group id in case of success, otherwise - throw SQLException */
    private static Long getGroupId(Connection connection, String group_name) throws SQLException {
        PreparedStatement pstmt = connection.prepareStatement("SELECT id FROM Groups WHERE  name = (?)");
        pstmt.setString(1, group_name);

        ResultSet results = null;
        try {
            results = pstmt.executeQuery();
            results.next();
            Long group_id = results.getLong(1);
            return group_id;
        } finally {
            pstmt.close();
        }
    }

    /*
     * Creates group, returns auto-generated id in case of success. Otherwise - exception is thrown
     */
    private static Long createGroup(Connection connection, String group_name) throws SQLException {
        PreparedStatement pstmt = null;
        Long group_id = null;

        try {
            pstmt = connection.prepareStatement(
                    "INSERT INTO Groups (name) VALUES (?);",
                    Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, group_name);
            pstmt.execute();

            ResultSet generatedKeys = pstmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                group_id = generatedKeys.getLong(1);
                return group_id;
            } else {
                group_id = getGroupId(connection, group_name);
            }
        } catch (SQLException e) {
            if (sqlStateMatches(e, UNIQUE_VIOLATION)) { // Group existed before - return it's id
                group_id = getGroupId(connection, group_name);
            } else {
                throw e;
            }
        } finally {
            close_statement(pstmt);
        }

        return group_id;
    }

    /**
     * Deletes a student from the database
     * Deleting a student will cause him\her to leave their group, delete their posts and likes history, and friendships
     * input: student
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if student does not exist
     * ERROR in case of database error
     */
    public static ReturnValue deleteStudent(Integer studentId) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(
                    "DELETE FROM Students " +
                            "WHERE id = ?");
            pstmt.setInt(1, studentId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ReturnValue.NOT_EXISTS;
            }
        } catch (SQLException e) {
            return ReturnValue.ERROR;
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }


    /**
     * Returns the student profile by the given id
     * input: student id
     * output: The student profile in case the student exists. BadStudent otherwise
     */

    public static Student getStudentProfile(Integer studentId) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement("SELECT Students.id, Students.name, Groups.name " +
                    "FROM (Students INNER JOIN Groups ON Students.faculty_id = Groups.id) " +
                    "WHERE Students.id = (?)");
            pstmt.setInt(1, studentId);

            ResultSet results = pstmt.executeQuery();
            Student s = new Student();
            if (results.next()) {
                s.setId(results.getInt(1));
                s.setName(results.getString(2));
                s.setFaculty(results.getString(3));
                return s;
            }

            results.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close_statement(pstmt);
            close_connection(connection);
        }
        return Student.badStudent();
    }


    /**
     * Updates a student faculty to the new given value.
     * The student should join the group of the new faculty, and stay in the old faculty’s group.
     * input: updated student
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if student does not exist
     * BAD_PARAMS in case of illegal parameters
     * ERROR in case of database error
     */
    public static ReturnValue updateStudentFaculty(Student student) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        ReturnValue result = null;
        try {
            pstmt = connection.prepareStatement(
                    "UPDATE Students " +
                            "SET faculty_id=(SELECT id FROM groups WHERE name=?)\n" +
                            "WHERE id=?;");
            pstmt.setString(1, student.getFaculty());
            pstmt.setInt(2, student.getId());

            // join new faculty (if nonexistent - will be created)
            // if student doesn't exist - joinGroup will catch this.
            result = joinGroup(student.getId(), student.getFaculty());
            if (!result.equals(ReturnValue.OK))
                return result;

            pstmt.executeUpdate();

        } catch (SQLException e) {
            return ReturnValue.ERROR;
        } finally {
            result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }


    /**
     * Adds a post to the database, and adds it to the relevant group if  groupName is given (i.e., it is not null)
     * When a student can write a post in a group only if he\she is one of its members
     * input: post to be posted
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * BAD_PARAMS in case of illegal parameters
     * NOT_EXISTS if student is not a member in the group
     * ALREADY_EXISTS if post already exists
     * ERROR in case of database error
     */
    public static ReturnValue addPost(Post post, String groupName) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            if (groupName != null) {
                pstmt = connection.prepareStatement(
                        "INSERT INTO Posts(id, author, group_id, contents, pdate)\n" +
                                "SELECT \n" +
                                "    ?, ?, (SELECT id FROM Groups WHERE  name = (?)), ?, ? \n" +
                                "    FROM Members WHERE student_id=? " +
                                "    AND group_id=(SELECT id FROM Groups WHERE  name = (?));");
                pstmt.setInt(1, post.getId());
                pstmt.setInt(2, post.getAuthor());
                pstmt.setString(3, groupName);
                pstmt.setString(4, post.getText());
                pstmt.setTimestamp(5, post.getTimeStamp());
                pstmt.setInt(6, post.getAuthor());
                pstmt.setString(7, groupName);
            } else {
                pstmt = connection.prepareStatement(
                        "INSERT INTO Posts(id, author, contents, pdate)\n" +
                                "VALUES (?, ?, ?, ?);");
                pstmt.setInt(1, post.getId());
                pstmt.setInt(2, post.getAuthor());
                pstmt.setString(3, post.getText());
                pstmt.setTimestamp(4, post.getTimeStamp());
            }
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ReturnValue.NOT_EXISTS;
            }
        } catch (SQLException e) {
            return sqlExceptionToReturnValue(e);
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }


    /**
     * Deletes a post from the database
     * input: post to be deleted
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if post does not exist
     * ERROR in case of database error
     */
    public static ReturnValue deletePost(Integer postId) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(
                    "DELETE FROM Posts " +
                            "WHERE id=?");
            pstmt.setInt(1, postId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ReturnValue.NOT_EXISTS;
            }
        } catch (SQLException e) {
            return ReturnValue.ERROR;
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }


    /**
     * returns the post by given id
     * input: post id
     * output: Post if the post exists. BadPost otherwise
     */
    public static Post getPost(Integer postId) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement("SELECT id, author, contents, pdate, " +
                    "(SELECT COUNT(*) FROM Likes WHERE  post_id = (?))" +
                    "FROM Posts " +
                    "WHERE id = (?)");
            pstmt.setInt(1, postId);
            pstmt.setInt(2, postId);

            ResultSet results = pstmt.executeQuery();
            Post p = new Post();
            if (results.next()) {
                p.setId(results.getInt(1));
                p.setAuthor(results.getInt(2));
                p.setText(results.getString(3));
                p.setTimeStamp(results.getTimestamp(4));
                p.setLikes(results.getInt(5));
                return p;
            }

            results.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            finalizePrintExceptionStack(connection, pstmt);
        }
        return Post.badPost();
    }

    /**
     * Updates a post’s text
     * input: updated post
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if post does not exist
     * BAD_PARAMS in case of illegal parameters
     * ERROR in case of database error
     */
    public static ReturnValue updatePost(Post post) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(
                    "UPDATE Posts " +
                            "SET contents=?\n" +
                            "WHERE id=? " + ";");
            pstmt.setString(1, post.getText());
            pstmt.setInt(2, post.getId());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ReturnValue.NOT_EXISTS;
            }
        } catch (SQLException e) {
            if (sqlStateMatches(e, CHECK_VIOLATION) || sqlStateMatches(e, NOT_NULL_VIOLATION)) {
                return ReturnValue.BAD_PARAMS;
            } else {
                return ReturnValue.ERROR;
            }
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }


    /**
     * Establishes a friendship relationship between two different students
     * input: student id 1, student id 2
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if one or two of the students do not exist
     * ALREADY_EXISTS if the students are already friends
     * BAD_PARAMS in case of illegal parameters
     * ERROR in case of database error
     */

    public static ReturnValue makeAsFriends(Integer studentId1, Integer studentId2) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement("INSERT INTO Friends (id1, id2) " +
                    " VALUES (?, ?), (?, ?)");
            pstmt.setInt(1, studentId1);
            pstmt.setInt(2, studentId2);
            pstmt.setInt(3, studentId2);
            pstmt.setInt(4, studentId1);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            return sqlExceptionToReturnValue(e);
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }


    /**
     * Removes a friendship connection of two students
     * input: student id 1, student id 2
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if one or two of the students do not exist,  or they are not labeled as friends
     * ERROR in case of database error
     */
    public static ReturnValue makeAsNotFriends(Integer studentId1, Integer studentId2) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(
                    "DELETE FROM Friends " +
                            "where (id1 = ? AND id2 = ?) OR (id2 = ? AND id1 = ?)");
            pstmt.setInt(1, studentId1);
            pstmt.setInt(2, studentId2);
            pstmt.setInt(3, studentId1);
            pstmt.setInt(4, studentId2);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ReturnValue.NOT_EXISTS;
            }
        } catch (SQLException e) {
            return ReturnValue.ERROR;
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }

    /**
     * Marks a post as liked by a student
     * input: student id, liked post id
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if student or post do not exist
     * ALREADY_EXISTS if the student is already likes the post
     * ERROR in case of database error
     */
    public static ReturnValue likePost(Integer studentId, Integer postId) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement("INSERT INTO likes(post_id, student_id)\n" +
                    "SELECT ?, ?\n" +
                    "WHERE EXISTS (\n" +
                    "        SELECT * FROM posts p ,members m \n" +
                    "        -- student is member of a group, or post is public\n" +
                    "        WHERE (m.group_id=p.group_id OR p.group_id IS NULL) \n" +
                    "        -- post and student exist\n" +
                    "        AND p.id=? AND m.student_id=? \n" +
                    "    );");
            pstmt.setInt(1, postId);
            pstmt.setInt(2, studentId);
            pstmt.setInt(3, postId);
            pstmt.setInt(4, studentId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ReturnValue.NOT_EXISTS;
            }
        } catch (SQLException e) {
            return sqlExceptionToReturnValue(e);
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }

    /**
     * Removes the like marking of a post by the student
     * input: student id, unliked post id
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if student or post do not exist,  or the student did not like the post
     * ERROR in case of database error
     */
    public static ReturnValue unlikePost(Integer studentId, Integer postId) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(
                    "DELETE FROM Likes " +
                            "where student_id = ? AND post_id = ?");
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, postId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ReturnValue.NOT_EXISTS;
            }
        } catch (SQLException e) {
            return ReturnValue.ERROR;
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }

    /**
     * Adds a student to a group (LP: if group doesn't exist - create it)
     * input: id of student to be added, the group name the student is added to
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if the student does not exist
     * ALREADY_EXISTS if the student are already in that group
     * ERROR in case of database error
     */
    public static ReturnValue joinGroup(Integer studentId, String groupName) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            createGroup(connection, groupName); // if group exists - no effect
            pstmt = connection.prepareStatement(
                    "INSERT INTO Members(group_id, student_id) " +
                            "VALUES (" +
                            "(SELECT id FROM Groups WHERE  name = (?))" +
                            ", ?)");
            pstmt.setString(1, groupName);
            pstmt.setInt(2, studentId);
            pstmt.execute();
        } catch (SQLException e) {
            return sqlExceptionToReturnValue(e);
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }

    /**
     * Removes a student from a group
     * input: student id, group name
     * output: ReturnValue with the following conditions:
     * OK in case of success
     * NOT_EXISTS if the student is not a member of the group
     * ERROR in case of database error
     */
    public static ReturnValue leaveGroup(Integer studentId, String groupName) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(
                    "DELETE FROM Members " +
                            "WHERE " +
                            "group_id=(SELECT id FROM Groups WHERE  name = (?))" +
                            " AND student_id=?");
            pstmt.setString(1, groupName);
            pstmt.setInt(2, studentId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                return ReturnValue.NOT_EXISTS;
            }
        } catch (SQLException e) {
            return ReturnValue.ERROR;
        } finally {
            ReturnValue result = finalize(connection, pstmt);
            if (!result.equals(ReturnValue.OK))
                return result;
        }
        return ReturnValue.OK;
    }


    /**
     * Gets a list of personal posts posted by a student and his\her friends. Feed should be ordered by date and likes, both in descending order.
     * input: student id
     * output: Feed the containing the relevant posts. In case of an error, return an empty feed
     */
    public static Feed getStudentFeed(Integer id) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        Feed feed = new Feed();
        try {
            pstmt = connection.prepareStatement("SELECT id, author, (SELECT COUNT(*) FROM Likes WHERE post_id = id) AS likes, contents, pdate " +
                    "FROM Posts " +
                    "   WHERE author IN (SELECT id2 FROM Friends WHERE  id1 = (?))" +
                    "       OR author=?" +
                    "ORDER BY pdate DESC, likes DESC ");
            pstmt.setInt(1, id);
            pstmt.setInt(2, id);
            ResultSet results = pstmt.executeQuery();


            while (results.next()) {
                Post p = new Post();
                p.setId(results.getInt(1));
                p.setAuthor(results.getInt(2));
                p.setLikes(results.getInt(3));
                p.setText(results.getString(4));
                p.setTimeStamp(results.getTimestamp(5));
                feed.add(p);
            }

            results.close();


        } catch (SQLException e) {
            return new Feed();
        } finally {
            try {
                pstmt.close();
            } catch (SQLException e) {
                return new Feed();
            }
            try {
                connection.close();
            } catch (SQLException e) {
                return new Feed();
            }
        }
        return feed;
    }

    /**
     * Gets a list of posts posted in a group. Feed should be ordered by date and likes, both in descending order.
     * input: group
     * output: Feed the containing the relevant posts. In case of an error, return an empty feed
     */

    public static Feed getGroupFeed(String groupName) {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        Feed feed = new Feed();
        try {
            pstmt = connection.prepareStatement("SELECT id, author, (SELECT COUNT(*) FROM Likes WHERE post_id = id), contents, pdate " +
                    "FROM Posts " +
                    "   WHERE group_id = (SELECT id FROM Groups WHERE  name = (?))" +
                    "ORDER BY 5 DESC, 3 DESC ");
            pstmt.setString(1, groupName);
            ResultSet results = pstmt.executeQuery();


            while (results.next()) {
                Post p = new Post();
                p.setId(results.getInt(1));
                p.setAuthor(results.getInt(2));
                p.setLikes(results.getInt(3));
                p.setText(results.getString(4));
                p.setTimeStamp(results.getTimestamp(5));
                feed.add(p);
            }

            results.close();


        } catch (SQLException e) {
            return new Feed();
        } finally {
            try {
                pstmt.close();
            } catch (SQLException e) {
                return new Feed();
            }
            try {
                connection.close();
            } catch (SQLException e) {
                return new Feed();
            }
        }
        return feed;
    }

    /**
     * Gets a list of students that the given student may know.
     * Denote the given the student by s. The returned list should consist of every student x in the database that holds the following:
     * - s ≠ x.
     * - s and x are not friends.
     * - There exists a student y such that y ≠ s, y ≠ x, s and y are friends, and y and x are friends.
     * - There exists a group such that both s and x are members of.
     * input: student
     * output: an ArrayList containing the students. In case of an error, return an empty ArrayList
     */
    public static ArrayList<Student> getPeopleYouMayKnowList(Integer studentId) {

        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        ArrayList<Student> students = new ArrayList<>();
        try {
            pstmt = connection.prepareStatement(
                    "SELECT id, name, (SELECT name From Groups WHERE id = Students.faculty_id) FROM Students \n" +
                    "WHERE id IN -- friends of 2nd degree \n" +
                    "( \n" +
                    "    SELECT id2 FROM Friends   \n" +
                    "    WHERE id1 IN (SELECT id2 FROM Friends WHERE id1 = (?)) \n" +
                    "      AND id2 <> (?) \n" +
                    "      AND id2 NOT IN (SELECT id2 FROM Friends WHERE id1 = (?)) \n" +
                    "      AND EXISTS -- some commmon group \n" +
                    "      (\n" +
                    "            SELECT group_id FROM Members \n" +
                    "            WHERE student_id = (?) \n" +
                    "              AND group_id IN (SELECT group_id FROM Members WHERE student_id = id2) \n" +
                    "        )\n" +
                    ");");

            pstmt.setInt(1, studentId);
            pstmt.setInt(2, studentId);
            pstmt.setInt(3, studentId);
            pstmt.setInt(4, studentId);
            ResultSet results = pstmt.executeQuery();


            while (results.next()) {
                Student s = new Student();
                s.setId(results.getInt(1));
                s.setName(results.getString(2));
                s.setFaculty(results.getString(3));
                students.add(s);
            }

            results.close();


        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
        return students;
    }

    /**
     * Returns a list of student id pairs (s1, s2) such that the degrees of separation (definition follows)
     * between s1 and s2 is at least 5.
     * To define the notion of degrees of separation let us consider a graph, called the friendship graph,
     * where its nodes are the students in the database, and there is an edge between two students iff they are friends.
     * The degrees of separation between students s1 and s2 is defined as the length of the shortest path
     * connecting s1 and s2 in the undirected friendship graph.
     * input: none
     * output: an ArrayList containing the student pairs. In case of an error, return an empty ArrayList
     */
    public static ArrayList<StudentIdPair> getRemotelyConnectedPairs() {
        Connection connection = DBConnector.getConnection();
        PreparedStatement pstmt = null;
        ArrayList<StudentIdPair> pairs = new ArrayList<>();
        try {
            pstmt = connection.prepareStatement(
                    "WITH RECURSIVE traverse(id1, id2, distance, visited) AS \n" +
                    "(\n" +
                    " 	SELECT id1, id2, 1, ARRAY[id1] \n" +
                    "	FROM Friends f \n" +
                    "UNION ALL\n" +
                    " 	SELECT t.id1, f.id2, t.distance + 1, t.visited || f.id2\n" +
                    "	FROM Friends f, traverse t \n" +
                    "	WHERE f.id1 = t.id2 AND NOT f.id2=ANY(t.visited) \n" +
                    ") \n" +
                    "SELECT id1, id2 \n" +
                    "FROM traverse \n" +
                    "WHERE id1>id2 \n" + // eliminate duplicates such as: (a,b) (b,a)
                    "GROUP BY id1, id2 \n" +
                    "HAVING MIN(distance) >= 5;");

            ResultSet results = pstmt.executeQuery();
            while (results.next()) {
                StudentIdPair p = new StudentIdPair();
                p.setStudentId1(results.getInt(1));
                p.setStudentId2(results.getInt(2));
                pairs.add(p);
            }
            results.close();

        } catch (SQLException e) {
            return new ArrayList<>();
        } finally {
            try {
                pstmt.close();
            } catch (SQLException e) {
                return new ArrayList<>();
            }
            try {
                connection.close();
            } catch (SQLException e) {
                return new ArrayList<>();
            }
        }
        return pairs;
    }

    private static boolean sqlStateMatches(SQLException e, PostgreSQLErrorCodes errorCode) {
        String errorString = Integer.toString(errorCode.getValue());
        return e.getSQLState().equals(errorString);
    }

    private static ReturnValue finalize(Connection connection, PreparedStatement pstmt) {
        try {
            if (pstmt != null)
                pstmt.close();
        } catch (SQLException e) {
            return ReturnValue.ERROR;
        }
        try {
            if (connection!=null)
                connection.close();
        } catch (SQLException e) {
            return ReturnValue.ERROR;
        }
        return ReturnValue.OK;
    }

    private static void finalizePrintExceptionStack(Connection connection, PreparedStatement pstmt) {
        close_statement(pstmt);
        close_connection(connection);
    }

    private static void close_statement(PreparedStatement pstmt) {
        try {
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void close_connection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* Suitable for most cases of handling SQLException */
    private static ReturnValue sqlExceptionToReturnValue(SQLException e) {
        if (sqlStateMatches(e, FOREIGN_KEY_VIOLATION)) {
            return ReturnValue.NOT_EXISTS;
        } else if (sqlStateMatches(e, UNIQUE_VIOLATION)) {
            return ReturnValue.ALREADY_EXISTS;
        } else if (sqlStateMatches(e, CHECK_VIOLATION) || sqlStateMatches(e, NOT_NULL_VIOLATION)) {
            return ReturnValue.BAD_PARAMS;
        } else {
            return ReturnValue.ERROR;
        }
    }

}


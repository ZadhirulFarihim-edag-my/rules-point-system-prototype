-- Clear existing data
DELETE FROM activity_points;
DELETE FROM group_point_history;
DELETE FROM point_history;
DELETE FROM persons;
DELETE FROM "groups";

-- Insert groups
INSERT INTO "groups" (id, name, total_group_points) VALUES ('group1', 'The Avengers', 0);
INSERT INTO "groups" (id, name, total_group_points) VALUES ('group2', 'Justice League', 0);
INSERT INTO "groups" (id, name, total_group_points) VALUES ('group3', 'Guardians of the Galaxy', 0);
INSERT INTO "groups" (id, name, total_group_points) VALUES ('group4', 'X-Men', 0);
INSERT INTO "groups" (id, name, total_group_points) VALUES ('group5', 'Fantastic Four', 0);

-- Insert persons
-- Group 1: The Avengers
INSERT INTO persons (id, name, group_id) VALUES ('person1', 'Iron Man', 'group1');
INSERT INTO persons (id, name, group_id) VALUES ('person2', 'Captain America', 'group1');
INSERT INTO persons (id, name, group_id) VALUES ('person3', 'Thor', 'group1');
INSERT INTO persons (id, name, group_id) VALUES ('person4', 'Hulk', 'group1');

-- Group 2: Justice League
INSERT INTO persons (id, name, group_id) VALUES ('person5', 'Superman', 'group2');
INSERT INTO persons (id, name, group_id) VALUES ('person6', 'Batman', 'group2');
INSERT INTO persons (id, name, group_id) VALUES ('person7', 'Wonder Woman', 'group2');
INSERT INTO persons (id, name, group_id) VALUES ('person8', 'Flash', 'group2');

-- Group 3: Guardians of the Galaxy
INSERT INTO persons (id, name, group_id) VALUES ('person9', 'Star-Lord', 'group3');
INSERT INTO persons (id, name, group_id) VALUES ('person10', 'Gamora', 'group3');
INSERT INTO persons (id, name, group_id) VALUES ('person11', 'Drax', 'group3');
INSERT INTO persons (id, name, group_id) VALUES ('person12', 'Rocket', 'group3');

-- Group 4: X-Men
INSERT INTO persons (id, name, group_id) VALUES ('person13', 'Wolverine', 'group4');
INSERT INTO persons (id, name, group_id) VALUES ('person14', 'Professor X', 'group4');
INSERT INTO persons (id, name, group_id) VALUES ('person15', 'Cyclops', 'group4');
INSERT INTO persons (id, name, group_id) VALUES ('person16', 'Storm', 'group4');

-- Group 5: Fantastic Four
INSERT INTO persons (id, name, group_id) VALUES ('person17', 'Mr. Fantastic', 'group5');
INSERT INTO persons (id, name, group_id) VALUES ('person18', 'Invisible Woman', 'group5');
INSERT INTO persons (id, name, group_id) VALUES ('person19', 'Human Torch', 'group5');
INSERT INTO persons (id, name, group_id) VALUES ('person20', 'The Thing', 'group5');
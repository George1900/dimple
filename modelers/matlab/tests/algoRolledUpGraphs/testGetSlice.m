function testGetSlice()

    %TODO: how do we test?
    s = BitStream();

    %test getSlice with only one arg
    s1 = s.getSlice(1);
    s2 = s.getSlice(2);
    s3 = s.getSlice(3);

    s1.getNext();
    s1.getNext();
    s1var = s1.getNext();
    s2.getNext();
    s2var = s2.getNext();
    s3var = s3.getNext();

    assertTrue(s1var == s2var);
    assertTrue(s2var == s3var);


    %test getSlice with two args
    ss = s.getSlice(1,3);
    ss.getNext();
    ss.getNext();

    assertTrue(ss.hasNext());

    ss.getNext();

    assertFalse(ss.hasNext());

    %test getSlice with three args
    s1 = s.getSlice(2,2,5);
    s2 = s.getSlice(3,2,6);

    s1var = s1.getNext();
    s2var = s2.getNext();


    assertTrue(s1var == s.get(2));
    assertTrue(s2var == s.get(3));

    s1var = s1.getNext();
    s2var = s2.getNext();

    assertTrue(s1var == s.get(4));
    assertTrue(s2var == s.get(5));
    
    assertFalse(s1.hasNext);
    assertFalse(s2.hasNext);
end

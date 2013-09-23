declare
  type strings is table of varchar(160);
  commands strings;
  cmd varchar(160);
begin
  --
  select cmd
  bulk collect into commands
  from
    (
    select 'drop type "'||type_name||'" force' as cmd,
           1 as ord, 0 as rnum
      from user_types
    union all
    select 'drop table "'||table_name||'" cascade constraints' as cmd,
           2 as ord, rnum
      from user_tables
           natural join
           (select object_name as table_name, object_id as rnum from user_objects where object_type = 'TABLE')
    union all
    select 'drop view "'||view_name||'"' as cmd,
           3 as ord, rnum
      from user_views
        natural join
        (select object_name as view_name, object_id as rnum from user_objects where object_type = 'VIEW')
    )
  order by ord desc, rnum desc;
  --
  if commands.count > 0 then
    for i in commands.first .. commands.last
      loop
        cmd := commands(i);
        execute immediate cmd;
      end loop;
  end if;
  --
end;

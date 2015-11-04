.PHONY: get-root new-post

get-root:
	curl http://localhost:9000/

new-post-wo-auth:
	curl -X PUT -H "Content-Type: application/json" -d '{"title":"Yet another title","text":"Lorem ipsum"}' http://localhost:9000/post

new-post:
	curl -u admin:demo -X PUT -H "Content-Type: application/json" -d '{"title":"Yet another title","text":"Lorem ipsum"}' http://localhost:9000/post

get-post:
	curl http://localhost:9000/post/1

new-comment:
	curl -X PUT -H "Content-Type: application/json" -d '{"text":"Lorem ipsum"}' http://localhost:9000/post/1/comment

new-comment-with-author:
	curl -X PUT -H "Content-Type: application/json" -d '{"author":"Incognito","text":"Lorem ipsum"}' http://localhost:9000/post/1/comment

patch-post:
	curl -u admin:demo -X PATCH -H "Content-Type: application/json" -d '{"text":"New text"}' http://localhost:9000/post/1

delete-post:
	curl -u admin:demo -X DELETE -H "Content-Type: application/json" http://localhost:9000/post/4

delete-comment:
	curl -u admin:demo -X DELETE -H "Content-Type: application/json" http://localhost:9000/post/1/comment/2

package com.example.postingapp.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.postingapp.entity.Post;
import com.example.postingapp.entity.User;
import com.example.postingapp.form.PostEditForm;
import com.example.postingapp.form.PostRegisterForm;
import com.example.postingapp.security.UserDetailsImpl;
import com.example.postingapp.service.PostService;

@Controller
// クラスに@RequestMappingアノテーションを付けると[https://ドメイン/posts/〇〇〇となり、各メソッドに共通のパスを繰り返し記述する必要がなくなる
@RequestMapping("/posts")
public class PostController {
	private final PostService postService;
	
	public PostController(PostService postService) {
		this.postService = postService;
	}
	// [/posts]が自動的にマッピングされる
	@GetMapping
	// 現在ログイン中のユーザ情報を取得すUserDetailsインターフェースを実装したオブジェクト(今回はUserDetailsImplクラスに@AuthenticationPrincipalをつける
	public String index(@AuthenticationPrincipal UserDetailsImpl userDetailsImpl, Model model) {
		User user = userDetailsImpl.getUser();
		List<Post> posts = postService.findPostsByUserOrderedByUpdatedAtAsc(user);
		
		model.addAttribute("posts", posts);
		
		return "posts/index";
	}
	
	@GetMapping("/{id}")
	public String show(@PathVariable(name = "id") Integer id, RedirectAttributes redirectAttributes,
			Model model) {
		Optional<Post> optionalPost = postService.findPostById(id);
		
		if (optionalPost.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "投稿が存在しません。");
			
			return "redirect:/posts";
		}
		
		Post post = optionalPost.get();
		model.addAttribute("post", post);
		
		return "posts/show";
	}
	
	@GetMapping("/register")
	public String register(Model model) {
		// フォームの各入力項目とフォームクラスの各フォール度をバインドするためコントローラーからビューにフォームクラスのインスタンスを渡す
		model.addAttribute("postRegisterForm", new PostRegisterForm());
		
		return "posts/register";
	}
	
	@PostMapping("/create")
	public String create(@ModelAttribute @Validated PostRegisterForm postRegisterForm,
			BindingResult bindingResult,
			//AuthenticationPrincipal をつけた引数に現在の認証ユーザ情報が自動的に渡される
			@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
			RedirectAttributes redirectAttributes,
			Model model)
	{
		if (bindingResult.hasErrors()) {
			model.addAttribute("postRegisterForm", postRegisterForm);
			
			return "posts/register";
		}
		
		User user = userDetailsImpl.getUser();
		
		postService.createPost(postRegisterForm, user);
		redirectAttributes.addFlashAttribute("successMessage", "投稿が完了しました。");
		
		return "redirect:/posts";
	}
	
	@GetMapping("/{id}/edit")
	public String edit(@PathVariable(name = "id") Integer id,
			@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
			RedirectAttributes redirectAttributes,
			Model model)
	{
		Optional<Post> optionalPost = postService.findPostById(id);
		// ログイン中のUserオブジェクトを取得
		User user = userDetailsImpl.getUser();
		
		if(optionalPost.isEmpty() || !optionalPost.get().getUser().equals(user)) {
			redirectAttributes.addFlashAttribute("errorMessage", "不正なアクセスです。");
			
			return "redirect:/posts";
		}
		
		Post post = optionalPost.get();
		model.addAttribute("post", post);
		model.addAttribute("postEditForm", new PostEditForm(post.getTitle(), post.getContent()));
		
		return "posts/edit";
	}
	
	@PostMapping("/{id}/update")
	public String update(@ModelAttribute @Validated PostEditForm postEditForm,
			BindingResult bindingResult,
			@PathVariable(name = "id") Integer id,
			@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
			RedirectAttributes redirectAttributes,
			Model model)
	{
		Optional<Post> optionalPost = postService.findPostById(id);
		User user = userDetailsImpl.getUser();
		
		// URLのidに一致する投稿が存在しない場合＋
		// その投稿を作成したユーザーがログイン中のユーザと一致しない場合も投稿一覧ページにリダイレクト
		if (optionalPost.isEmpty() || !optionalPost.get().getUser().equals(user)) {
			redirectAttributes.addFlashAttribute("errorMessage", "不正なアクセスです。");
			
			return "redirect:/posts";
		}
		
		Post post = optionalPost.get();
		
		if (bindingResult.hasErrors()) {
			model.addAttribute("post", post);
			model.addAttribute("postEditForm", postEditForm);
			
			return "posts/edit";
		}
		
		postService.updatePost(postEditForm,  post);
		redirectAttributes.addFlashAttribute("successMessage", "投稿を編集しました。");
		
		return "redirect:/posts/" + id;
	}
	
	@PostMapping("/{id}/delete")
	public String delete(@PathVariable(name = "id") Integer id,
			@AuthenticationPrincipal UserDetailsImpl userDetailsImpl,
			RedirectAttributes redirectAttributes,
			Model model)
	{
		Optional<Post> optionalPost = postService.findPostById(id);
		User user = userDetailsImpl.getUser();
		
		if (optionalPost.isEmpty() || !optionalPost.get().getUser().equals(user)) {
			redirectAttributes.addFlashAttribute("errorMessage", "不正なアクセスです。");
			
			return "redirect:/posts";
		}
		
		Post post = optionalPost.get();
		postService.deletePost(post);
		redirectAttributes.addFlashAttribute("successMessage", "投稿を削除しました。");
		
		return "redirect:/posts";
	}
}

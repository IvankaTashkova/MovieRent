package movierent.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import movierent.model.Movie;
import movierent.model.User;
import movierent.model.dao.MovieDao;
import movierent.model.dao.UserDao;

@Controller
public class UserController {
	
	@Autowired
	private UserDao userDao;
	@Autowired
	MovieDao movieDao;
	
	@RequestMapping(value = {"/login"},method = RequestMethod.POST)
	public String login(Model model,@RequestParam String email,
			@RequestParam String password,HttpSession session) throws SQLException {
		//check if user is in db
		if(userDao.checkUser(email,password)) {
			// login
			User user = userDao.getUserByEmail(email);
			System.out.println(user.getMoney());
			session.setAttribute("user", user);
			//redirect
			ArrayList<Movie> movies = movieDao.movies();
			model.addAttribute("movies", movies);
			if(user.isAdmin()) {
				return "admin";
			}
			return "home";
		}
		//redirect
		model.addAttribute("msg", "Wrong email or password!");
		return "index";
		
	}
	
	@RequestMapping(value = {"/logout"},method = RequestMethod.POST)
	public String logout(HttpSession session) {
		//clear session
		session.invalidate();
		//redirect
		return "index";
	}
	
	@RequestMapping(value = {"/register"},method = RequestMethod.GET)
	public String getRegister() {
		
		return "register";
	}
	
	@RequestMapping(value = {"/register"},method = RequestMethod.POST)
	public String register(Model model,
			@RequestParam String firstname, 
			@RequestParam String lastname,
			@RequestParam String email,
			@RequestParam String password,
			@RequestParam String confirmpassword,
			HttpSession session) throws SQLException {
		//check if email is not in db
		if(userDao.checkEmailIfExist(email)) {
			model.addAttribute("msg", "This email is already used!");
			return "register";
		}
		//check is password and confirmpassword match
		if(!(password.equals(confirmpassword))) {
			model.addAttribute("msg", "Passwords doesn't match!");
			return "register";
		}
		//create user
		User user =  new User(firstname, lastname, email, password,false);
		//insert in db
		userDao.register(user);
		System.out.println("inserted in db");	
		//redirect
		model.addAttribute("msg", "Successful registration! You can now log in!");
		return "index";
	}
	
	@RequestMapping(value = {"/profile"},method = RequestMethod.GET)
	public String getProfile(Model model,HttpSession session) throws SQLException {
		User user = (User) session.getAttribute("user");
		if(user == null) {
			return "index";
		}
		ArrayList<Movie> rented = movieDao.rentedMovies(user);
		model.addAttribute("rented", rented);
		ArrayList<Movie>bought =  movieDao.boughtMovies(user);
		model.addAttribute("bought",bought);
		ArrayList<Movie> favorites =  movieDao.favorites(user);
		model.addAttribute("favorites", favorites);
		return "profile";
	}
	
	@RequestMapping(value = {"/profile"},method = RequestMethod.POST)
	public String getPaymentResponse(Model model,HttpSession session, @RequestParam String amount) throws IOException, SQLException {
		URL url = new URL("http://localhost:8080/Payment/payment");
		URLConnection connection =  url.openConnection();
		((HttpURLConnection) connection).setRequestMethod("GET");
		InputStream responseBodyStream = connection.getInputStream();
		int b = responseBodyStream.read();
		StringBuffer sb = new StringBuffer();
		while(b != -1) {
			sb.append((char)b);
			b = responseBodyStream.read();
		}
		String json = sb.toString();
		System.out.println("Result" +json);
		model.addAttribute("msg", json);
		User user = (User) session.getAttribute("user");
		if(user == null) {
			return "index";
		}
		ArrayList<Movie> rented = movieDao.rentedMovies(user);
		model.addAttribute("rented", rented);
		ArrayList<Movie>bought =  movieDao.boughtMovies(user);
		model.addAttribute("bought",bought);
		ArrayList<Movie> favorites =  movieDao.favorites(user);
		model.addAttribute("favorites", favorites);
		
		if(json.toLowerCase().contains("success")) {
			System.out.println("SUCCESS");
			double money = Double.parseDouble(amount);
			System.out.println(money);
			double currentAmount = user.getMoney();
			user.setMoney(currentAmount+money);
			userDao.changeUserAmount(user);
		}
		return "profile";
	}
	
	
}

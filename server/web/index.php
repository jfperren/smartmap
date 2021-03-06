<?php

require_once __DIR__.'/../vendor/autoload.php';

use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\HttpFoundation\Request;

use Monolog\Logger;
use Monolog\Handler\StreamHandler;

use Silex\Application;

$app = new Application();

// Loading options
$options = json_decode(file_get_contents(__DIR__ . '/../config.json'), true);

$app['debug'] = $options['debug'];

// Database connection
$app->register(new Silex\Provider\DoctrineServiceProvider(), array(
    'db.options' => array(
        'driver' => $options['db']['driver'],
        'host' => $options['db']['host'],
        'dbname' => $options['db']['dbname'],
        'user' => $options['db']['user'],
        'password' => $options['db']['password'],
        'charset' => $options['db']['charset']
    )
));

// Starting session service
$app->register(new Silex\Provider\SessionServiceProvider());


// Injecting repositories
$app['user.repository'] = $app->share(function() use($app) {
    return new SmartMap\DBInterface\UserRepository($app['db']);
});

$app['event.repository'] = $app->share(function() use($app) {
    return new SmartMap\DBInterface\EventRepository($app['db']);
});


// Starting logging service
$app['logging'] = $app->share(function() use($app, $options) {
    $logger = new Logger('logging');
    $logger->pushHandler(new StreamHandler('../' . $options['monolog']['logfile'], Logger::INFO));
   return $logger;
});

// Injecting controllers
$app->register(new Silex\Provider\ServiceControllerServiceProvider());

$app['authentication.controller'] = $app->share(function() use($app, $options) {
    return new SmartMap\Control\AuthenticationController($app['user.repository'],
                                                         $options['facebook']['appId'],
                                                         $options['facebook']['appSecret']);
});

$app['authorization.controller'] = $app->share(function() use($app) {
    return new SmartMap\Control\AuthorizationController($app['user.repository']);
});

$app['data.controller'] = $app->share(function() use($app) {
    return new SmartMap\Control\DataController($app['user.repository']);
});

$app['profile.controller'] = $app->share(function() use($app) {
    return new SmartMap\Control\ProfileController();
});

$app['event.controller'] = $app->share(function() use($app) {
    return new SmartMap\Control\EventController($app['event.repository'], $app['user.repository']);
});


// Error management
$app->error(function (SmartMap\Control\InvalidRequestException $e, $code) use ($app) {
    $app['logging']->addWarning('Invalid request: ' . $e->__toString());
    return new JsonResponse(array('status' => 'error', 'message' => $e->getMessage()), 200,
        array('X-Status-Code' => 200));
});

$app->error(function (SmartMap\Control\ServerFeedbackException $e, $code) use ($app) {
    $app['logging']->addWarning('Invalid request with feedback: ' . $e->__toString());
    return new JsonResponse(array('status' => 'feedback', 'message' => $e->getMessage()), 200,
        array('X-Status-Code' => 200));
});

$app->error(function (Symfony\Component\Routing\Exception\RouteNotFoundException $e, $code) use ($app) {
    $app['logging']->addWarning('Request for invalid route: ' . $e->__toString());
    return new JsonResponse(array('status' => 'error', 'message' => 'Invalid URI.'), 200,
        array('X-Status-Code' => 200));
});


$app->error(function (SmartMap\Control\ControlLogicException $e, $code) use ($app) {
    $app['logging']->addError($e->__toString());
    if ($app['debug'] == true) {
        return;
    }
    return new JsonResponse(array('status' => 'error', 'message' => 'An internal server error occurred.'), 500,
        array('X-Status-Code' => 500));
});

$app->error(function (\Exception $e, $code) use ($app) {
    $app['logging']->addCritical('Unexpected exception: ' . $e->__toString());
    if ($app['debug'] == true) {
        return;
    }
    return new JsonResponse(array('status' => 'error', 'message' => 'An internal error occurred'), 500,
        array('X-Status-Code' => 500));
});


// Routing
$app->post('/auth', 'authentication.controller:authenticate');

$app->post('/registerUser', 'authentication.controller:registerUser');

$app->post('/allowFriend', 'authorization.controller:allowFriend');

$app->post('/disallowFriend', 'authorization.controller:disallowFriend');

$app->post('/allowFriendList', 'authorization.controller:allowFriendList');

$app->post('/disallowFriendList', 'authorization.controller:disallowFriendList');

$app->post('/blockFriend', 'authorization.controller:blockFriend');

$app->post('/unblockFriend', 'authorization.controller:unblockFriend');

$app->post('/setVisibility', 'authorization.controller:setVisibility');

$app->post('/getUserInfo', 'data.controller:getUserInfo');

$app->post('/getProfilePicture', 'profile.controller:getProfilePicture');

$app->post('/inviteFriend', 'data.controller:inviteFriend');

$app->post('/getInvitations', 'data.controller:getInvitations');

$app->post('/acceptInvitation', 'data.controller:acceptInvitation');

$app->post('/declineInvitation', 'data.controller:declineInvitation');

$app->post('/ackAcceptedInvitation', 'data.controller:ackAcceptedInvitation');

$app->post('/ackRemovedFriend', 'data.controller:ackRemovedFriend');

$app->post('/removeFriend', 'data.controller:removeFriend');

$app->post('/listFriendsPos', 'data.controller:listFriendsPos');

$app->post('/updatePos', 'data.controller:updatePos');

$app->post('/findUsers', 'data.controller:findUsers');

$app->post('/getFriendsIds', 'data.controller:getFriendsIds');

$app->post('/createEvent', 'event.controller:createEvent');

$app->post('/updateEvent', 'event.controller:updateEvent');

$app->post('/getPublicEvents', 'event.controller:getPublicEvents');

$app->post('/joinEvent', 'event.controller:joinEvent');

$app->post('/leaveEvent', 'event.controller:leaveEvent');

$app->post('/inviteUsersToEvent', 'event.controller:inviteUsersToEvent');

$app->post('/getEventInvitations', 'event.controller:getEventInvitations');

$app->post('/ackEventInvitation', 'event.controller:ackEventInvitation');

$app->post('/getEventInfo', 'event.controller:getEventInfo');

// Easy authentication for testing
if ($app['debug'] == true)
{
    $app->post('/fakeAuth', 'authentication.controller:fakeAuth');
}

// Logging of requests
$app->before(function(Request $request, Application $app) {
    $app['logging']->addInfo('New request: ' . $request->getRequestUri() .
        ' from ip ' . $request->getClientIp() . '.');
});

$app->run();
